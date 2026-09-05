-- Idempotency key for transaction creation (Fase 12).
-- A client-sent key makes createTransaction replay-safe: the same key can only
-- ever produce one active transaction row.
ALTER TABLE "transactions" ADD COLUMN IF NOT EXISTS "idempotency_key" VARCHAR(100) DEFAULT NULL;

-- Partial unique index: only active rows are constrained, so soft-deleted
-- rows (deleted_at IS NOT NULL) do not block a new transaction with the same key.
CREATE UNIQUE INDEX IF NOT EXISTS "idx_transactions_idempotency_key_active"
    ON "transactions" ("idempotency_key")
    WHERE "idempotency_key" IS NOT NULL AND "deleted_at" IS NULL;
