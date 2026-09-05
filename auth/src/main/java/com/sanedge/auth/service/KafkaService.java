package com.sanedge.auth.service;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class KafkaService {
    private static final Logger log = LoggerFactory.getLogger(KafkaService.class);

    @Inject
    Vertx vertx;

    @Inject
    com.sanedge.common.chaos.ChaosManager chaosManager;

    @Inject
    OpenTelemetry openTelemetry;

    Tracer tracer;
    TextMapPropagator propagator;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    String bootstrapServers;

    @ConfigProperty(name = "kafka.acks", defaultValue = "all")
    String acks;

    @ConfigProperty(name = "kafka.idempotence", defaultValue = "true")
    boolean idempotence;

    volatile KafkaProducer<String, String> producer;

    @PostConstruct
    void init() {
        // Fase 14 (K-6, K-7): acks=all + enable.idempotence=true + optional
        // SASL/SCRAM & TLS from KAFKA_* env vars, via the shared KafkaConfig.
        Map<String, String> config = com.sanedge.common.config.KafkaConfig.producer(
                bootstrapServers, acks, idempotence, com.sanedge.common.config.KafkaSecurity.fromEnv());
        producer = KafkaProducer.create(vertx, config);
        this.tracer = openTelemetry.getTracer("auth-kafka-producer", "1.0.0");
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
        log.info("✅ KafkaProducer initialized. brokers={} acks={} idempotence={}",
                bootstrapServers, acks, idempotence);
    }

    @PreDestroy
    void destroy() {
        if (producer != null) {
            producer.close()
                    .onSuccess(v -> log.info("✅ KafkaProducer closed."))
                    .onFailure(err -> log.warn("⚠️ Error closing KafkaProducer: {}", err.getMessage()));
        }
    }

    public Uni<Void> sendMessage(String topic, String key, JsonObject payload) {
        if (producer == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Kafka producer not initialized"));
        }
        if (payload == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Kafka payload cannot be null"));
        }
        // Phase 2 (event contract): attach the standard envelope
        // (event_id, schema_version, event_type, occurred_at) before publishing.
        JsonObject eventPayload = com.sanedge.common.event.EventEnvelope.withDefaults(payload, topic);

        // Producer span (Fase 13): the W3C traceparent is injected into the record
        // headers so the email consumer becomes a CHILD_OF this span in Jaeger.
        Span span = tracer.spanBuilder("kafka.produce")
                .setSpanKind(SpanKind.PRODUCER)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination", topic)
                .setAttribute("messaging.destination.name", topic)
                .setAttribute("messaging.operation", "publish")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            com.sanedge.common.chaos.ChaosPolicy policy = chaosManager.evaluate("kafka", topic);
            if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
                log.info("🔥 Injecting Kafka chaos [Policy: {}] for topic: {}", policy.getName(), topic);

                if (policy.getLatencyMs() > 0) {
                    long delay = policy.getLatencyMs();
                    return Uni.createFrom().emitter(emitter -> {
                        vertx.setTimer(delay, id -> {
                            if (policy.isDropMessage()) {
                                log.info("💧 Silent drop (latency + dropMessage) for Kafka topic: {}", topic);
                                span.end();
                                emitter.complete(null);
                            } else if (policy.isRejectMessage()) {
                                log.warn("❌ Rejecting message (latency + rejectMessage) for Kafka topic: {}", topic);
                                failSpan(span, policy.getErrorMessage());
                                emitter.fail(new RuntimeException(
                                        policy.getErrorMessage() != null ? policy.getErrorMessage()
                                                : "Simulated Kafka drop/reject error"));
                            } else {
                                KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key,
                                        eventPayload.encode());
                                injectTraceparent(record);
                                sendWithSpan(producer, record, span, emitter);
                            }
                        });
                    });
                } else {
                    if (policy.isDropMessage()) {
                        log.info("💧 Silent drop (dropMessage) for Kafka topic: {}", topic);
                        span.end();
                        return Uni.createFrom().voidItem();
                    } else if (policy.isRejectMessage()) {
                        log.warn("❌ Rejecting message (rejectMessage) for Kafka topic: {}", topic);
                        failSpan(span, policy.getErrorMessage());
                        return Uni.createFrom().failure(new RuntimeException(
                                policy.getErrorMessage() != null ? policy.getErrorMessage()
                                        : "Simulated Kafka drop/reject error"));
                    }
                }
            }

            KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, eventPayload.encode());
            injectTraceparent(record);
            return Uni.createFrom().emitter(emitter -> sendWithSpan(producer, record, span, emitter));
        }
    }

    private void sendWithSpan(KafkaProducer<String, String> producer, KafkaProducerRecord<String, String> record,
            Span span, io.smallrye.mutiny.subscription.UniEmitter<? super Void> emitter) {
        producer.send(record)
                .onSuccess(metadata -> {
                    log.debug("✅ Sent to topic={} partition={} offset={}",
                            record.topic(), metadata.getPartition(), metadata.getOffset());
                    span.end();
                    emitter.complete(null);
                })
                .onFailure(err -> {
                    log.error("❌ Failed to send to topic={}: {}", record.topic(), err.getMessage());
                    failSpan(span, err.getMessage());
                    emitter.fail(err);
                });
    }

    private void failSpan(Span span, String message) {
        span.setStatus(StatusCode.ERROR, message != null ? message : "Kafka send failed");
        span.end();
    }

    /**
     * Injects the current W3C trace context into the record headers as
     * {@code traceparent}, so the consumer can continue the same trace.
     */
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
}
