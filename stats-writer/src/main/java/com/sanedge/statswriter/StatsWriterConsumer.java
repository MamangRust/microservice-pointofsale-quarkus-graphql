package com.sanedge.statswriter;

import com.sanedge.common.clickhouse.ClickHouseClient;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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
 * Kafka consumer for the stats topics (transactions.event, order.event,
 * order_item.event) using Vert.x Kafka client
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
    private final ConcurrentLinkedQueue<Map<String, Object>> orderItemBatch = new ConcurrentLinkedQueue<>();
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
        java.util.Set<String> topics = java.util.Set.of(
                com.sanedge.common.event.EventTopics.ORDER,
                com.sanedge.common.event.EventTopics.ORDER_ITEM,
                com.sanedge.common.event.EventTopics.TRANSACTIONS);
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

        if (com.sanedge.common.event.EventTopics.ORDER.equals(topic)) {
            Map<String, Object> row = extractOrderRow(json);
            if (row != null) {
                orderBatch.add(row);
                eventsConsumed.incrementAndGet();
                if (orderBatch.size() >= BATCH_SIZE) {
                    flushOrderBatch();
                }
            }
        } else if (com.sanedge.common.event.EventTopics.ORDER_ITEM.equals(topic)) {
            Map<String, Object> row = extractOrderItemRow(json);
            if (row != null) {
                orderItemBatch.add(row);
                eventsConsumed.incrementAndGet();
                if (orderItemBatch.size() >= BATCH_SIZE) {
                    flushOrderItemBatch();
                }
            }
        } else if (com.sanedge.common.event.EventTopics.TRANSACTIONS.equals(topic)) {
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
        JsonObject body = body(json);
        Map<String, Object> row = new HashMap<>();
        row.put("event_id", json.getString("event_id"));
        row.put("occurred_at", ClickHouseClient.normalizeDateTime(json.getString("occurred_at")));
        row.put("order_id", asString(body, "order_id"));
        row.put("merchant_id", asString(body, "merchant_id"));
        row.put("cashier_id", asString(body, "cashier_id"));
        row.put("status", asStatus(body.getString("status"), "created"));
        row.put("total_amount", asLong(body, "total_amount", 0L));
        row.put("event_version", System.currentTimeMillis());
        return row;
    }

    private Map<String, Object> extractTransactionRow(io.vertx.core.json.JsonObject json) {
        JsonObject body = body(json);
        Map<String, Object> row = new HashMap<>();
        row.put("event_id", json.getString("event_id"));
        row.put("occurred_at", ClickHouseClient.normalizeDateTime(json.getString("occurred_at")));
        row.put("transaction_id", asString(body, "transaction_id"));
        row.put("order_id", asString(body, "order_id"));
        row.put("merchant_id", asString(body, "merchant_id"));
        row.put("cashier_id", asString(body, "cashier_id"));
        row.put("payment_method", body.getString("payment_method"));
        row.put("status", asStatus(body.getString("status"), "unknown"));
        row.put("amount", asLong(body, "amount", 0L));
        row.put("event_version", System.currentTimeMillis());
        return row;
    }

    private Map<String, Object> extractOrderItemRow(io.vertx.core.json.JsonObject json) {
        JsonObject body = body(json);
        Map<String, Object> row = new HashMap<>();
        row.put("event_id", json.getString("event_id"));
        row.put("occurred_at", ClickHouseClient.normalizeDateTime(json.getString("occurred_at")));
        row.put("order_item_id", asString(body, "order_item_id"));
        row.put("order_id", asString(body, "order_id"));
        row.put("product_id", asString(body, "product_id"));
        row.put("quantity", asInt(body, "quantity", 0));
        // The order_item event carries the unit price as "unit_price".
        row.put("price", asLong(body, "price", asLong(body, "unit_price", 0L)));
        row.put("subtotal", asLong(body, "subtotal", 0L));
        row.put("event_version", System.currentTimeMillis());
        return row;
    }

    /**
     * Returns the event body: the nested {@code payload} object when the event
     * is published directly (order.created / order_item.created /
     * transaction.created), otherwise the flat envelope itself (outbox events).
     */
    private static JsonObject body(JsonObject json) {
        JsonObject payload = json.getJsonObject("payload");
        return payload != null ? payload : json;
    }

    private static String asString(JsonObject json, String key) {
        Object value = json.getValue(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static Long asLong(JsonObject json, String key, Long defaultValue) {
        Object value = json.getValue(key);
        return value instanceof Number ? ((Number) value).longValue() : defaultValue;
    }

    private static Integer asInt(JsonObject json, String key, int defaultValue) {
        Object value = json.getValue(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    /** Normalizes the status so the same state is not split by casing. */
    private static String asStatus(String status, String defaultValue) {
        return status == null || status.isBlank()
                ? defaultValue
                : status.toLowerCase();
    }

    private void flushAll() {
        flushOrderBatch();
        flushOrderItemBatch();
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

    private void flushOrderItemBatch() {
        List<Map<String, Object>> batch = new ArrayList<>();
        while (!orderItemBatch.isEmpty() && batch.size() < BATCH_SIZE) {
            Map<String, Object> row = orderItemBatch.poll();
            if (row != null) batch.add(row);
        }
        if (!batch.isEmpty()) {
            clickHouseClient.insert("pos.order_item_daily", batch)
                    .thenRun(() -> log.debug("✅ Flushed {} order item rows to ClickHouse", batch.size()))
                    .exceptionally(ex -> {
                        log.error("❌ Failed to flush order item batch: {}", ex.getMessage(), ex);
                        flushErrors.incrementAndGet();
                        batch.forEach(orderItemBatch::add);
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
    public int getOrderItemBatchSize() { return orderItemBatch.size(); }
}
