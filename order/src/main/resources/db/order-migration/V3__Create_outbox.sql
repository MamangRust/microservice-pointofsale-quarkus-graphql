-- Outbox table for reliable Kafka event publishing (transactional outbox pattern).
-- Producers write business entity + outbox row in the SAME DB transaction,
-- then an OutboxPublisher service pushes PENDING rows to Kafka and marks them
-- PROCESSED (or FAILED after max attempts).
CREATE TABLE IF NOT EXISTS "pos_order"."outbox" (
    "id" BIGINT NOT NULL DEFAULT nextval('pos_order.outbox_seq') PRIMARY KEY,
    "aggregate_type" VARCHAR(100) NOT NULL,
    "aggregate_id" VARCHAR(100) NOT NULL,
    "topic" VARCHAR(200) NOT NULL,
    "payload" TEXT NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "attempts" INT NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "processed_at" TIMESTAMP DEFAULT NULL,
    "last_error" TEXT DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS "idx_outbox_status" ON "pos_order"."outbox" ("status");
CREATE INDEX IF NOT EXISTS "idx_outbox_status_created" ON "pos_order"."outbox" ("status", "created_at");
