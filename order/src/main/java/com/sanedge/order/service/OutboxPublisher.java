package com.sanedge.order.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.order.entity.Outbox;
import com.sanedge.order.repository.OutboxRepository;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Transactional outbox publisher for the order module. Polls PENDING outbox
 * rows on a fixed interval and pushes them to Kafka. This publisher handles
 * both order domain events and stats events (order.created, order_item.created).
 */
@ApplicationScoped
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    @Inject
    Vertx vertx;

    @Inject
    OutboxRepository outboxRepository;

    @Inject
    KafkaService kafkaService;

    @ConfigProperty(name = "outbox.publisher.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "outbox.publisher.interval-ms", defaultValue = "5000")
    long intervalMs;

    @ConfigProperty(name = "outbox.publisher.batch-size", defaultValue = "50")
    int batchSize;

    @ConfigProperty(name = "outbox.publisher.max-attempts", defaultValue = "5")
    int maxAttempts;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Long timerId;

    private final LongCounter publishedCounter;
    private final LongCounter failedCounter;

    @Inject
    public OutboxPublisher(OpenTelemetry openTelemetry) {
        io.opentelemetry.api.metrics.Meter meter = openTelemetry.getMeter("order-outbox");
        this.publishedCounter = meter.counterBuilder("outbox_published_total")
                .setDescription("Total outbox rows successfully published to Kafka")
                .build();
        this.failedCounter = meter.counterBuilder("outbox_failed_total")
                .setDescription("Total outbox rows that reached max attempts (dead letter)")
                .build();
    }

    void onStart(@Observes StartupEvent ev) {
        if (!enabled) {
            log.info("⏸️ OutboxPublisher disabled (outbox.publisher.enabled=false)");
            return;
        }
        timerId = vertx.setPeriodic(intervalMs, id -> publishPending());
        log.info("🚀 OutboxPublisher started | interval={}ms batchSize={} maxAttempts={}",
                intervalMs, batchSize, maxAttempts);
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (timerId != null) {
            vertx.cancelTimer(timerId);
        }
    }

    void publishPending() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        drainPending().subscribe().with(
                v -> running.set(false),
                err -> {
                    log.error("❌ OutboxPublisher tick failed: {}", err.getMessage(), err);
                    running.set(false);
                });
    }

    Uni<Void> drainPending() {
        return outboxRepository.findPending(batchSize)
                .chain(this::processPending);
    }

    private Uni<Void> processPending(List<Outbox> pending) {
        if (pending.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        return process(pending.get(0))
                .chain(() -> processPending(pending.subList(1, pending.size())));
    }

    private Uni<Void> process(Outbox outbox) {
        // Use sendExistingEvent to preserve the original occurred_at from the payload
        return kafkaService.sendExistingEvent(
                        outbox.getTopic(),
                        outbox.getAggregateId(),
                        outbox.getPayload())
                .chain(() -> outboxRepository.markProcessed(outbox.getId()))
                .onItem().invoke(() -> {
                    publishedCounter.add(1);
                    log.debug("📤 Outbox published | id={} topic={} key={} domain={}",
                            outbox.getId(), outbox.getTopic(), outbox.getAggregateId(), outbox.getDomain());
                })
                .onFailure().recoverWithUni(err -> {
                    log.warn("⚠️ Outbox publish failed | id={} topic={} error={}",
                            outbox.getId(), outbox.getTopic(), err.getMessage());
                    return outboxRepository.markFailed(outbox.getId(), err.getMessage(), maxAttempts)
                            .invoke(() -> {
                                if (outbox.getAttempts() + 1 >= maxAttempts) {
                                    failedCounter.add(1);
                                    log.error("❌ Outbox moved to FAILED (dead letter) | id={} topic={} attempts={}",
                                            outbox.getId(), outbox.getTopic(), outbox.getAttempts() + 1);
                                }
                            });
                });
    }
}
