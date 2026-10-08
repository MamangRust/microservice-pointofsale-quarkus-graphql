-- Transactional outbox for the auth (identity) publisher. In the 5-instance
-- topology each publisher context owns its outbox table; auth writes its outbox
-- rows in the identity database (pos_identity, public schema) alongside the
-- business entity, then the relay publishes PENDING rows to Kafka.
CREATE SEQUENCE IF NOT EXISTS "outbox_seq";

CREATE TABLE IF NOT EXISTS "outbox" (
    "id" BIGINT NOT NULL DEFAULT nextval('outbox_seq') PRIMARY KEY,
    "aggregate_type" VARCHAR(100) NOT NULL,
    "aggregate_id" VARCHAR(100) NOT NULL,
    "topic" VARCHAR(200) NOT NULL,
    "payload" TEXT NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "attempts" INT NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "processed_at" TIMESTAMP DEFAULT NULL,
    "last_error" TEXT DEFAULT NULL,
    "domain" VARCHAR(50),
    "event_id" VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS "idx_outbox_status" ON "outbox" ("status");
CREATE INDEX IF NOT EXISTS "idx_outbox_status_created" ON "outbox" ("status", "created_at");

-- Partial unique index: only enforce uniqueness for non-SENT rows so SENT rows
-- don't block a retry with the same event_id.
CREATE UNIQUE INDEX IF NOT EXISTS "idx_outbox_event_id_active"
    ON "outbox" ("event_id")
    WHERE "event_id" IS NOT NULL AND "status" != 'SENT';
