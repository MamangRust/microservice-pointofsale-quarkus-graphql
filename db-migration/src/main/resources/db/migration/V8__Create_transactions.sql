CREATE TABLE IF NOT EXISTS "transactions" (
    "transaction_id" SERIAL PRIMARY KEY,
    "order_id" INT NOT NULL REFERENCES "orders" ("order_id") ON DELETE CASCADE,
    "merchant_id" INT NOT NULL REFERENCES "merchants" ("merchant_id") ON DELETE CASCADE,
    "payment_method" VARCHAR(50) NOT NULL,
    "amount" INT NOT NULL,
    "change_amount" INT DEFAULT 0,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE SEQUENCE IF NOT EXISTS transactions_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_transactions_order_id" ON "transactions" ("order_id");
CREATE INDEX IF NOT EXISTS "idx_transactions_merchant_id" ON "transactions" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_transactions_status" ON "transactions" ("status");
