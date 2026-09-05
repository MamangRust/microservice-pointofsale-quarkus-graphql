-- ClickHouse stats schema — run via stats-writer init or manually.
-- These tables use ReplacingMergeTree for idempotent at-least-once ingestion.

CREATE DATABASE IF NOT EXISTS pos_stats;

-- Order daily stats (F3 events → ClickHouse)
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

-- Transaction daily stats (F3 events → ClickHouse)
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
