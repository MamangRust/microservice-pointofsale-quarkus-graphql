-- ClickHouse stats schema for the pointofsale stats pipeline.
-- Mounted by deployments/local/docker-compose*.yml as ../../pkg/clickhouse/schema.sql
-- (=> <repo-root>/pkg/clickhouse/schema.sql), run by the ClickHouse entrypoint on first boot.
-- Tables use ReplacingMergeTree for idempotent at-least-once ingestion.

CREATE DATABASE IF NOT EXISTS pos_stats;

-- Order daily stats (stats.pos.order.event -> ClickHouse)
CREATE TABLE IF NOT EXISTS pos_stats.order_daily
(
    event_id      String,
    occurred_at   DateTime,
    order_id      String,
    merchant_id   String,
    cashier_id    Nullable(String),
    status        LowCardinality(String),
    total_amount  Decimal(18,2),
    event_version UInt64
) ENGINE = ReplacingMergeTree(event_version)
ORDER BY (toDate(occurred_at), order_id, event_id)
TTL toDate(occurred_at) + INTERVAL 2 YEAR;

-- Order item daily stats (stats.pos.order_item.event -> ClickHouse)
CREATE TABLE IF NOT EXISTS pos_stats.order_item_daily
(
    event_id      String,
    occurred_at   DateTime,
    order_item_id String,
    order_id      String,
    merchant_id   String,
    category_id   String,
    product_id    String,
    quantity      UInt32,
    unit_price    Decimal(18,2),
    subtotal      Decimal(18,2),
    event_version UInt64
) ENGINE = ReplacingMergeTree(event_version)
ORDER BY (toDate(occurred_at), order_item_id, event_id)
TTL toDate(occurred_at) + INTERVAL 2 YEAR;

-- Transaction daily stats (stats.pos.transaction.event -> ClickHouse)
CREATE TABLE IF NOT EXISTS pos_stats.transaction_daily
(
    event_id       String,
    occurred_at    DateTime,
    transaction_id String,
    order_id       String,
    merchant_id    String,
    cashier_id     Nullable(String),
    payment_method LowCardinality(String),
    status         LowCardinality(String),
    amount         Decimal(18,2),
    event_version  UInt64
) ENGINE = ReplacingMergeTree(event_version)
ORDER BY (toDate(occurred_at), transaction_id, event_id)
TTL toDate(occurred_at) + INTERVAL 2 YEAR;
