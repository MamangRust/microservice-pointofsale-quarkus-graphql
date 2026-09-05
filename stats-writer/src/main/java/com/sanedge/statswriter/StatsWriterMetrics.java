package com.sanedge.statswriter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicLong;

/**
 * OTel metrics for stats-writer (payment_quarkus F6 lessons).
 * Uses OTel Meter API (not micrometer) for consistency.
 */
@ApplicationScoped
public class StatsWriterMetrics {

    private final LongCounter eventsConsumed;
    private final LongCounter flushErrors;
    private final AtomicLong kafkaLag = new AtomicLong();
    private final AtomicLong batchPending = new AtomicLong();

    @Inject
    StatsWriterConsumer consumer;

    @Inject
    public StatsWriterMetrics(OpenTelemetry openTelemetry) {
        Meter meter = openTelemetry.getMeter("stats-writer");
        this.eventsConsumed = meter.counterBuilder("stats_writer_events_consumed_total")
                .setDescription("Total events consumed from Kafka")
                .build();
        this.flushErrors = meter.counterBuilder("stats_writer_flush_errors_total")
                .setDescription("Total flush errors to ClickHouse")
                .build();
        meter.gaugeBuilder("stats_writer_kafka_lag").ofLongs()
                .buildWithCallback(obs -> obs.record(kafkaLag.get()));
        meter.gaugeBuilder("stats_writer_batch_pending").ofLongs()
                .buildWithCallback(obs -> obs.record(batchPending.get()));
    }

    public void recordEventConsumed() { eventsConsumed.add(1); }
    public void recordFlushError() { flushErrors.add(1); }
    public void setKafkaLag(long lag) { kafkaLag.set(lag); }
    public void updateBatchPending() { batchPending.set(consumer.getOrderBatchSize() + consumer.getTransactionBatchSize()); }
}
