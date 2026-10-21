-- Transactional outbox hardening: lease/claim columns so that the several
-- replicas of a publisher never relay the same row twice.
--
-- The `outbox` table is shared by the modules of the sales context (pos_sales):
-- a row is owned by the module named in its `domain` column (added in V4), and
-- only that module's publisher claims it. A publisher claims a row by writing a
-- claim token plus the lease start; the lease expires so a crashed publisher
-- releases its rows for the next attempt.
ALTER TABLE "outbox" ADD COLUMN IF NOT EXISTS "event_key" VARCHAR(200);
ALTER TABLE "outbox" ADD COLUMN IF NOT EXISTS "next_attempt_at" TIMESTAMP NULL;
ALTER TABLE "outbox" ADD COLUMN IF NOT EXISTS "claimed_at" TIMESTAMP NULL;
ALTER TABLE "outbox" ADD COLUMN IF NOT EXISTS "claim_token" VARCHAR(100) NULL;

-- Rows written before the domain discriminator existed belong to the
-- transaction module (it is the only writer that did not set a domain).
UPDATE "outbox" SET "domain" = 'transaction' WHERE "domain" IS NULL;

CREATE INDEX IF NOT EXISTS "idx_outbox_due"
    ON "outbox" ("domain", "status", "next_attempt_at", "claimed_at");
