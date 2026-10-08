CREATE SEQUENCE IF NOT EXISTS "transactions_seq";

CREATE TABLE IF NOT EXISTS "transactions" (
    "transaction_id" BIGINT NOT NULL DEFAULT nextval('transactions_seq') PRIMARY KEY,
    "order_id" BIGINT NOT NULL REFERENCES "orders" ("order_id") ON DELETE CASCADE,
    "merchant_id" BIGINT NOT NULL,
    "payment_method" VARCHAR(50) NOT NULL,
    "amount" INT NOT NULL,
    "change_amount" INT DEFAULT 0,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_transactions_order_id" ON "transactions" ("order_id");
CREATE INDEX IF NOT EXISTS "idx_transactions_merchant_id" ON "transactions" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_transactions_status" ON "transactions" ("status");
