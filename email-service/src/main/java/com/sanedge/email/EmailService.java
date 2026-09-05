package com.sanedge.email;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaHeader;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Email worker (single Kafka consumer). Notifications are published by domain
 * services via the transactional outbox (Fase 11), consumed here and sent
 * through SMTP.
 *
 * <p>Delivery guarantees:
 * <ul>
 *   <li><b>Dedup</b> — {@link EmailDedupGuard} claims
 *       {@code email:idempotency:<event_id>} atomically with a lease BEFORE the
 *       send (Phase 3 state machine) and flips it to {@code SENT} after a
 *       successful delivery; replays/retries of the same event_id are skipped.</li>
 *   <li><b>Retry / DLQ</b> — a failed SMTP send is re-published to
 *       {@code email-service-topic-email-retry} with a backoff delay and an
 *       attempt counter; after {@code email.retry.max-attempts} the record is
 *       moved to {@code email-service-topic-email-dlq} instead of being lost.</li>
 * </ul>
 */
@ApplicationScoped
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public static final String RETRY_TOPIC = "email-service-topic-email-retry";
    public static final String DLQ_TOPIC = "email-service-topic-email-dlq";

    private static final List<String> INTERNAL_FIELDS = Arrays.asList(
            "_srcTopic", "_srcPartition", "_srcOffset", "_attempt", "_retryAt", "_reason");

    @Inject
    Vertx vertx;

    @Inject
    ReactiveMailer mailer;

    @Inject
    EmailDedupGuard dedupGuard;

    @ConfigProperty(name = "email.retry.max-attempts", defaultValue = "3")
    int maxAttempts;

    @ConfigProperty(name = "email.retry.base-backoff-ms", defaultValue = "30000")
    long baseBackoffMs;

    @ConfigProperty(name = "email.lag.poll-ms", defaultValue = "15000")
    long lagPollMs;

    KafkaConsumer<String, JsonObject> consumer;
    KafkaProducer<String, String> producer;
    private final Map<TopicPartition, CompletableFuture<Void>> partitionTails = new ConcurrentHashMap<>();

    // Phase 6: consumer-lag gauge for the email consumer group (partitionLag is
    // refreshed periodically and exposed as kafka_consumer_lag{group,partition}).
    private final Map<TopicPartition, Long> latestCommittedPosition = new ConcurrentHashMap<>();
    private final Map<String, Long> partitionLag = new ConcurrentHashMap<>();
    private volatile long lagTimerId = -1;

    private final LongCounter sentCounter;
    private final LongCounter retriedCounter;
    private final LongCounter dlqCounter;
    private final LongCounter failedCounter;
    private final LongCounter duplicateCounter;
    private final LongCounter invalidCounter;
    private final DoubleHistogram processingDuration;
    private final Tracer tracer;
    private final TextMapPropagator propagator;

    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier != null ? carrier.get(key) : null;
        }
    };

    @Inject
    public EmailService(OpenTelemetry openTelemetry) {
        io.opentelemetry.api.metrics.Meter meter = openTelemetry.getMeter("email-service");
        this.sentCounter = meter.counterBuilder("email_sent_total")
                .setDescription("Total emails successfully sent via SMTP")
                .build();
        this.retriedCounter = meter.counterBuilder("email_retried_total")
                .setDescription("Total emails routed to the retry topic after a failed send")
                .build();
        this.dlqCounter = meter.counterBuilder("email_dlq_total")
                .setDescription("Total emails moved to the dead-letter topic after max attempts")
                .build();
        this.failedCounter = meter.counterBuilder("email_failed_total")
                .setDescription("Total email send attempts that failed")
                .build();
        this.duplicateCounter = meter.counterBuilder("email_duplicate_total")
                .setDescription("Total duplicate email events skipped")
                .build();
        this.invalidCounter = meter.counterBuilder("email_invalid_event_total")
                .setDescription("Total events rejected for an invalid envelope")
                .build();
        this.processingDuration = meter.histogramBuilder("email_processing_duration_seconds")
                .setDescription("Email record processing duration in seconds")
                .setUnit("s")
                .build();
        // Phase 6: expose per-partition consumer lag. The callback runs on each
        // scrape; failures degrade gracefully (empty series = consumer not assigned).
        meter.gaugeBuilder("kafka_consumer_lag")
                .setDescription("Kafka consumer lag per partition for the email consumer group")
                .buildWithCallback(measurement -> partitionLag.forEach((partition, value) -> measurement.record(
                        value == null ? 0.0 : value.doubleValue(),
                        io.opentelemetry.api.common.Attributes.of(
                                io.opentelemetry.api.common.AttributeKey.stringKey("group"), "email-service-group",
                                io.opentelemetry.api.common.AttributeKey.stringKey("partition"), partition))));
        this.tracer = openTelemetry.getTracer("email-service", "1.0.0");
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    void onStart(@Observes StartupEvent ev) {
        log.info("📧 Starting Email Service...");

        producer = KafkaProducer.create(vertx, producerConfig());

        consumer = KafkaConsumer.create(vertx, consumerConfig());
        consumer.handler(this::handleRecord);

        List<String> topics = Arrays.asList(
                "email-service-topic-auth-register",
                "email-service-topic-auth-forgot-password",
                "email-service-topic-auth-verify-code-success",
                "email-service-topic-transaction-create",
                "email-service-topic-merchant-create",
                "email-service-topic-merchant-update-status",
                "email-service-topic-merchant-document-create",
                "email-service-topic-merchant-document-update-status");

        consumer.subscribe(new java.util.HashSet<>(topics))
                .onSuccess(v -> log.info("📧 Email Service subscribed to {} topics", topics.size()))
                .onFailure(err -> log.error("❌ Failed to subscribe Email Service", err));

        lagTimerId = vertx.setPeriodic(Math.max(1000, lagPollMs), ignored -> refreshLag());
    }

    /**
     * Refreshes {@link #partitionLag} for the current assignment: end offset minus
     * the last committed position per partition. Non-blocking; failures only log.
     */
    private void refreshLag() {
        if (consumer == null) {
            return;
        }
        consumer.assignment()
                .onSuccess(partitions -> {
                    if (partitions == null || partitions.isEmpty()) {
                        return;
                    }
                    consumer.endOffsets(partitions)
                            .onSuccess(endOffsets -> {
                                Map<String, Long> lag = new HashMap<>();
                                for (TopicPartition partition : partitions) {
                                    Long end = endOffsets.get(partition);
                                    if (end == null) {
                                        continue;
                                    }
                                    Long position = latestCommittedPosition.get(partition);
                                    long committed = position == null ? 0L : position;
                                    lag.put(partition.getTopic() + "-" + partition.getPartition(),
                                            Math.max(0L, end - committed));
                                }
                                partitionLag.clear();
                                partitionLag.putAll(lag);
                            })
                            .onFailure(err -> log.warn("⚠️ Failed to read Kafka end offsets for lag metric", err));
                })
                .onFailure(err -> log.warn("⚠️ Failed to read Kafka assignment for lag metric", err));
    }

    void handleRecord(io.vertx.kafka.client.consumer.KafkaConsumerRecord<String, JsonObject> record) {
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        CompletableFuture<Void> previous = partitionTails.getOrDefault(partition,
                CompletableFuture.completedFuture(null));

        Span span = startConsumeSpan(record.headers(), record.topic(), record.partition(), record.offset());
        CompletableFuture<Void> current;
        try (Scope scope = span.makeCurrent()) {
            current = previous
                    .thenCompose(ignored -> processAndCommit(record)
                            // Keep records in a partition ordered. A transient SMTP/Kafka
                            // failure must not allow a later offset to be committed.
                            .onFailure().retry()
                            .withBackOff(Duration.ofMillis(250), Duration.ofSeconds(5))
                            .indefinitely()
                            .subscribeAsCompletionStage())
                    .whenComplete((ignored, error) -> {
                        if (error != null) {
                            span.setStatus(StatusCode.ERROR, error.getMessage());
                            log.error("❌ Error processing record | topic={} partition={} offset={} error={}",
                                    record.topic(), record.partition(), record.offset(), error.getMessage());
                        }
                        span.end();
                    });
        } catch (Exception error) {
            span.recordException(error);
            span.end();
            log.error("❌ Unhandled error in consumer handler", error);
            return;
        }
        partitionTails.put(partition, current);
    }

    private Uni<Void> processAndCommit(io.vertx.kafka.client.consumer.KafkaConsumerRecord<String, JsonObject> record) {
        long startNanos = System.nanoTime();
        return processRecord(record.topic(), record.partition(), record.offset(),
                record.key(), record.value(), 0)
                .chain(ignored -> commit(record))
                .invoke(() -> processingDuration.record((System.nanoTime() - startNanos) / 1_000_000_000.0));
    }

    private Uni<Void> commit(io.vertx.kafka.client.consumer.KafkaConsumerRecord<String, JsonObject> record) {
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        Map<TopicPartition, OffsetAndMetadata> offsets = Map.of(partition,
                new OffsetAndMetadata(record.offset() + 1, null));
        return Uni.createFrom().completionStage(consumer.commit(offsets).toCompletionStage())
                .replaceWithVoid()
                .invoke(() -> {
                    latestCommittedPosition.put(partition, record.offset() + 1);
                    log.debug("✅ Committed email offset topic={} partition={} offset={}",
                            record.topic(), record.partition(), record.offset());
                });
    }

    void onStop(@Observes ShutdownEvent ev) {
        partitionTails.clear();
        latestCommittedPosition.clear();
        partitionLag.clear();
        if (lagTimerId >= 0) {
            vertx.cancelTimer(lagTimerId);
        }
        if (consumer != null) {
            consumer.close()
                    .onSuccess(v -> log.info("📧 Kafka consumer closed"))
                    .onFailure(err -> log.error("❌ Failed to close Kafka consumer", err));
        }
        if (producer != null) {
            producer.close()
                    .onSuccess(v -> log.info("📧 Kafka producer closed"))
                    .onFailure(err -> log.error("❌ Failed to close Kafka producer", err));
        }
    }

    /**
     * Process one logical notification. Dedup is checked first; a successful
     * send claims the dedup key; a failed send is routed to retry (or DLQ once
     * max attempts is reached). Package-private for direct unit testing.
     */
    Uni<Void> processRecord(String srcTopic, int srcPartition, long srcOffset, String key,
            JsonObject payload, int attempt) {
        if (payload == null) {
            log.warn("⚠️ Received null payload, skipping");
            return Uni.createFrom().voidItem();
        }
        if (!hasValidEnvelope(payload)) {
            // Phase 2 (event contract): invalid events never reach SMTP. They are
            // parked in the DLQ (consistent with Ecommerce) instead of being
            // silently skipped.
            log.warn("⚠️ Invalid email envelope, routing to DLQ | topic={} partition={} offset={}",
                    srcTopic, srcPartition, srcOffset);
            invalidCounter.add(1);
            return routeToDeadLetter(payload, key, srcTopic, srcPartition, srcOffset, 0,
                    "invalid_event_envelope");
        }

        String eventId = payload.getString("event_id");
        return dedupGuard.claim(eventId)
                .chain(result -> {
                    switch (result) {
                        case DUPLICATE:
                            // Phase 3: terminal SENT state — replay/retry of the same
                            // event_id must never send a second email.
                            log.info("⏭️ Duplicate email skipped (event_id={})", eventId);
                            duplicateCounter.add(1);
                            return Uni.createFrom().voidItem();
                        case BUSY:
                            // Another consumer holds the lease. Do NOT commit: fail so
                            // the per-partition retry re-claims once the lease expires.
                            return Uni.createFrom().failure(new IllegalStateException(
                                    "Email idempotency claim busy, retrying event_id=" + eventId));
                        default: // CLAIMED
                            return sendEmail(payload)
                                    .chain(sent -> {
                                        if (sent) {
                                            return dedupGuard.markSent(eventId);
                                        }
                                        // Failed send: release the claim so the retry
                                        // topic can process the event again.
                                        return dedupGuard.release(eventId)
                                                .chain(ignored -> routeToRetry(payload, key,
                                                        srcTopic, srcPartition, srcOffset, attempt));
                                    });
                    }
                });
    }

    private Uni<Boolean> sendEmail(JsonObject payload) {
        String email = payload.getString("email");
        String subject = payload.getString("subject");
        String body = payload.getString("body");

        return mailer.send(Mail.withHtml(email, subject, body))
                .map(v -> {
                    log.info("✅ Email successfully sent to {}", email);
                    sentCounter.add(1);
                    return true;
                })
                .onFailure().recoverWithItem(err -> {
                    log.error("❌ Failed to send email to {}: {}", email, err.getMessage());
                    failedCounter.add(1);
                    return false;
                });
    }

    private Uni<Void> routeToRetry(JsonObject payload, String key, String srcTopic,
            int srcPartition, long srcOffset, int attempt) {
        int nextAttempt = attempt + 1;
        if (nextAttempt >= maxAttempts) {
            return routeToDeadLetter(payload, key, srcTopic, srcPartition, srcOffset, nextAttempt,
                    "max_retries_exceeded");
        }

        JsonObject retry = payload.copy()
                .put("_srcTopic", srcTopic)
                .put("_srcPartition", srcPartition)
                .put("_srcOffset", srcOffset)
                .put("_attempt", nextAttempt)
                .put("_retryAt", System.currentTimeMillis() + backoffMs(nextAttempt))
                .put("_reason", "smtp_send_failed");

        retriedCounter.add(1);
        return sendToTopic(RETRY_TOPIC, key, retry)
                .invoke(() -> log.warn("🔁 Email send failed; scheduled retry {} of {} | to={}",
                        nextAttempt, maxAttempts, payload.getString("email")));
    }

    private Uni<Void> routeToDeadLetter(JsonObject payload, String key, String srcTopic,
            int srcPartition, long srcOffset, int attempts, String reason) {
        JsonObject dlq = payload.copy();
        for (String field : INTERNAL_FIELDS) {
            dlq.remove(field);
        }
        dlq.put("_reason", reason)
                .put("_srcTopic", srcTopic)
                .put("_srcPartition", srcPartition)
                .put("_srcOffset", srcOffset)
                .put("_attempts", attempts);

        dlqCounter.add(1);
        return sendToTopic(DLQ_TOPIC, key, dlq)
                .invoke(() -> log.error("☠️ Email moved to DLQ after {} attempts | to={} topic={} reason={}",
                        attempts, payload.getString("email"), srcTopic, reason));
    }

    private Uni<Void> sendToTopic(String topic, String key, JsonObject payload) {
        return Uni.createFrom().emitter(emitter -> {
            KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, payload.encode());
            injectTraceparent(record);
            producer.send(record)
                    .onSuccess(metadata -> {
                        log.debug("📤 Sent to topic={} partition={} offset={}",
                                topic, metadata.getPartition(), metadata.getOffset());
                        emitter.complete(null);
                    })
                    .onFailure(err -> {
                        log.error("❌ Failed to publish to topic={}: {}", topic, err.getMessage());
                        emitter.fail(err);
                    });
        });
    }

    /**
     * Starts a {@code kafka.consume} span parented to the W3C trace context that
     * the producer injected into the record headers. Package-private so
     * {@link RetryProcessor} can reuse it for retry-topic records.
     */
    Span startConsumeSpan(java.util.List<KafkaHeader> headers, String topic, int partition, long offset) {
        Map<String, String> carrier = new HashMap<>();
        if (headers != null) {
            for (KafkaHeader header : headers) {
                if (header.value() != null) {
                    carrier.put(header.key(), header.value().toString());
                }
            }
        }
        Context parent = propagator.extract(Context.current(), carrier, MAP_GETTER);
        return tracer.spanBuilder("kafka.consume")
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(parent)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.source", topic)
                .setAttribute("messaging.destination.partition", partition)
                .setAttribute("messaging.kafka.offset", offset)
                .setAttribute("messaging.operation", "process")
                .startSpan();
    }

    private void injectTraceparent(KafkaProducerRecord<String, String> record) {
        try {
            Map<String, String> carrier = new HashMap<>();
            propagator.inject(Context.current(), carrier, Map::put);
            String traceparent = carrier.get("traceparent");
            if (traceparent != null) {
                record.addHeader("traceparent", traceparent);
            }
        } catch (Exception e) {
            log.debug("Failed to inject traceparent header: {}", e.getMessage());
        }
    }

    private boolean hasValidEnvelope(JsonObject payload) {
        // Phase 2 (event contract): envelope validation, consistent with the
        // Ecommerce consumer (event_id, schema_version=1, event_type).
        return payload != null
                && payload.getString("event_id") != null
                && payload.getInteger("schema_version", 0) == 1
                && payload.getString("event_type") != null
                && payload.getString("email") != null
                && payload.getString("subject") != null
                && payload.getString("body") != null;
    }

    private long backoffMs(int attempt) {
        return Math.min((long) attempt * baseBackoffMs, 300_000L);
    }

    private Map<String, String> consumerConfig() {
        // Fase 14 (K-6): SASL/SCRAM + TLS from KAFKA_* env vars (local helper,
        // email-service tidak depend pada module common).
        String bootstrap = System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092");
        return KafkaSecurityConfig.consumer(bootstrap, "email-service-group");
    }

    private Map<String, String> producerConfig() {
        // Fase 14 (K-6, K-7): acks=all + enable.idempotence=true (env-overridable)
        // + SASL/SCRAM & TLS from KAFKA_* env vars (local helper).
        String bootstrap = System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092");
        String acks = System.getenv().getOrDefault("KAFKA_ACKS", KafkaSecurityConfig.DEFAULT_ACKS);
        boolean idempotence = Boolean.parseBoolean(
                System.getenv().getOrDefault("KAFKA_IDEMPOTENCE", "true"));
        return KafkaSecurityConfig.producer(bootstrap, acks, idempotence);
    }
}
