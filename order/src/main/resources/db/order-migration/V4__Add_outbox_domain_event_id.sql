-- F3: Add domain and event_id columns to outbox for multi-domain event publishing.
-- The domain column identifies which module published the event (order, transaction, auth).
-- The event_id column provides idempotent deduplication (ReplacingMergeTree in ClickHouse).
ALTER TABLE "pos_order"."outbox" ADD COLUMN IF NOT EXISTS "domain" VARCHAR(50);
ALTER TABLE "pos_order"."outbox" ADD COLUMN IF NOT EXISTS "event_id" VARCHAR(100);

-- Partial unique index: only enforce uniqueness for non-SENT rows so SENT rows
-- don't block a retry with the same event_id.
CREATE UNIQUE INDEX IF NOT EXISTS "idx_outbox_event_id_active"
    ON "pos_order"."outbox" ("event_id")
    WHERE "event_id" IS NOT NULL AND "status" != 'SENT';
