-- Durable idempotency store for the email consumer (pos_email context).
-- Mirrors the Go DDL in service/email/internal/inbox/inbox.go. The
-- (consumer_name, event_key) primary key is the conflict target used by the
-- reservation SQL.
CREATE TABLE IF NOT EXISTS "consumer_inbox" (
    "consumer_name" VARCHAR(255) NOT NULL,
    "event_key" VARCHAR(255) NOT NULL,
    "topic" VARCHAR(255) NOT NULL DEFAULT '',
    "partition_id" INT NOT NULL DEFAULT 0,
    "message_offset" BIGINT NOT NULL DEFAULT 0,
    "status" VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK ("status" IN ('pending', 'processing', 'processed')),
    "attempts" INT NOT NULL DEFAULT 0,
    "reservation_version" BIGINT NOT NULL DEFAULT 0,
    "lease_until" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "last_error" TEXT NOT NULL DEFAULT '',
    "processed_at" TIMESTAMP,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("consumer_name", "event_key")
);

CREATE INDEX IF NOT EXISTS "idx_consumer_inbox_lease" ON "consumer_inbox"("status", "lease_until");
