package com.sanedge.statswriter;

import com.sanedge.common.clickhouse.ClickHouseClient;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka consumer for stats.pos.* topics using Vert.x Kafka client
 * (matching the existing project pattern in order/transaction modules).
 * Receives domain events and batches them for efficient ClickHouse insertion.
 * Flushes every 1000 rows or every 5 seconds.
 */
@ApplicationScoped
public class StatsWriterConsumer {

    private static final Logger log = LoggerFactory.getLogger(StatsWriterConsumer.class);
    private static final int BATCH_SIZE = 1000;
    private static final long FLUSH_INTERVAL_MS = 5000;

    @Inject
    Vertx vertx;

    @Inject
    ClickHouseClient clickHouseClient;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    String bootstrapServers;

    @ConfigProperty(name = "kafka.group.id", defaultValue = "pos-stats-writer")
    String groupId;

    private final ConcurrentLinkedQueue<Map<String, Object>> orderBatch = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Map<String, Object>> transactionBatch = new ConcurrentLinkedQueue<>();
    private final AtomicLong eventsConsumed = new AtomicLong(0);
    private final AtomicLong flushErrors = new AtomicLong(0);

    private KafkaConsumer<String, String> consumer;
    private ScheduledExecutorService scheduler;

    @PostConstruct
    void init() {
        // Create Kafka consumer
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("group.id", groupId);
        config.put("auto.offset.reset", "earliest");
        config.put("enable.auto.commit", "true");
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = KafkaConsumer.create(vertx, config);

        // Subscribe to topics
        java.util.Set<String> topics = java.util.Set.of("stats.pos.order.event", "stats.pos.transaction.event");
        consumer.subscribe(topics)
                .onSuccess(v -> log.info("✅ Subscribed to topics: {}", topics))
                .onFailure(e -> log.error("❌ Failed to subscribe: {}", e.getMessage(), e));

        // Poll loop
        vertx.setPeriodic(1000, id -> poll());

        // Flush scheduler
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::flushAll, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);

        log.info("🚀 StatsWriterConsumer started | bootstrap={} groupId={} batchSize={} flushInterval={}ms",
                bootstrapServers, groupId, BATCH_SIZE, FLUSH_INTERVAL_MS);
    }

    @PreDestroy
    void destroy() {
        if (consumer != null) {
            consumer.close();
        }
        if (scheduler != null) {
            flushAll();
            scheduler.shutdown();
        }
    }

    private void poll() {
        if (consumer == null) return;
        consumer.poll(Duration.ofMillis(500))
                .onSuccess(records -> {
                    for (int i = 0; i < records.size(); i++) {
                        var record = records.recordAt(i);
                        try {
                            processRecord(record.topic(), record.value());
                        } catch (Exception e) {
                            log.error("❌ Failed to process record from {}: {}", record.topic(), e.getMessage(), e);
                            flushErrors.incrementAndGet();
                        }
                    }
                })
                .onFailure(e -> log.error("❌ Poll failed: {}", e.getMessage(), e));
    }

    private void processRecord(String topic, String message) {
        io.vertx.core.json.JsonObject json = new io.vertx.core.json.JsonObject(message);

        if ("stats.pos.order.event".equals(topic)) {
            Map<String, Object> row = extractOrderRow(json);
            if (row != null) {
                orderBatch.add(row);
                eventsConsumed.incrementAndGet();
                if (orderBatch.size() >= BATCH_SIZE) {
                    flushOrderBatch();
                }
            }
        } else if ("stats.pos.transaction.event".equals(topic)) {
            Map<String, Object> row = extractTransactionRow(json);
            if (row != null) {
                transactionBatch.add(row);
                eventsConsumed.incrementAndGet();
                if (transactionBatch.size() >= BATCH_SIZE) {
                    flushTransactionBatch();
                }
            }
        }
    }

    private Map<String, Object> extractOrderRow(io.vertx.core.json.JsonObject json) {
        Map<String, Object> row = new HashMap<>();
        row.put("event_id", json.getString("event_id"));
        row.put("occurred_at", ClickHouseClient.normalizeDateTime(json.getString("occurred_at")));
        row.put("order_id", String.valueOf(json.getLong("order_id")));
        row.put("merchant_id", String.valueOf(json.getLong("merchant_id")));
        row.put("cashier_id", json.getLong("cashier_id") != null ? String.valueOf(json.getLong("cashier_id")) : null);
        row.put("status", json.getString("status", "created"));
        row.put("total_amount", json.getLong("total_amount", 0L));
        row.put("event_version", System.currentTimeMillis());
        return row;
    }

    private Map<String, Object> extractTransactionRow(io.vertx.core.json.JsonObject json) {
        Map<String, Object> row = new HashMap<>();
        row.put("event_id", json.getString("event_id"));
        row.put("occurred_at", ClickHouseClient.normalizeDateTime(json.getString("occurred_at")));
        row.put("transaction_id", String.valueOf(json.getLong("transaction_id")));
        row.put("order_id", String.valueOf(json.getLong("order_id")));
        row.put("merchant_id", String.valueOf(json.getLong("merchant_id")));
        row.put("cashier_id", json.getLong("cashier_id") != null ? String.valueOf(json.getLong("cashier_id")) : null);
        row.put("payment_method", json.getString("payment_method"));
        row.put("status", json.getString("status"));
        row.put("amount", json.getLong("amount", 0L));
        row.put("event_version", System.currentTimeMillis());
        return row;
    }

    private void flushAll() {
        flushOrderBatch();
        flushTransactionBatch();
    }

    private void flushOrderBatch() {
        List<Map<String, Object>> batch = new ArrayList<>();
        while (!orderBatch.isEmpty() && batch.size() < BATCH_SIZE) {
            Map<String, Object> row = orderBatch.poll();
            if (row != null) batch.add(row);
        }
        if (!batch.isEmpty()) {
            clickHouseClient.insert("pos.order_daily", batch)
                    .thenRun(() -> log.debug("✅ Flushed {} order rows to ClickHouse", batch.size()))
                    .exceptionally(ex -> {
                        log.error("❌ Failed to flush order batch: {}", ex.getMessage(), ex);
                        flushErrors.incrementAndGet();
                        batch.forEach(orderBatch::add);
                        return null;
                    });
        }
    }

    private void flushTransactionBatch() {
        List<Map<String, Object>> batch = new ArrayList<>();
        while (!transactionBatch.isEmpty() && batch.size() < BATCH_SIZE) {
            Map<String, Object> row = transactionBatch.poll();
            if (row != null) batch.add(row);
        }
        if (!batch.isEmpty()) {
            clickHouseClient.insert("pos.transaction_daily", batch)
                    .thenRun(() -> log.debug("✅ Flushed {} transaction rows to ClickHouse", batch.size()))
                    .exceptionally(ex -> {
                        log.error("❌ Failed to flush transaction batch: {}", ex.getMessage(), ex);
                        flushErrors.incrementAndGet();
                        batch.forEach(transactionBatch::add);
                        return null;
                    });
        }
    }

    public long getEventsConsumed() { return eventsConsumed.get(); }
    public long getFlushErrors() { return flushErrors.get(); }
    public int getOrderBatchSize() { return orderBatch.size(); }
    public int getTransactionBatchSize() { return transactionBatch.size(); }
}
