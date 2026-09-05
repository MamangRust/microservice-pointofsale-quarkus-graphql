CREATE TABLE IF NOT EXISTS "pos_transaction"."transactions" (
    "transaction_id" BIGINT NOT NULL DEFAULT nextval('pos_transaction.transactions_seq') PRIMARY KEY,
    "order_id" BIGINT NOT NULL REFERENCES "pos_order"."orders" ("order_id") ON DELETE CASCADE,
    "merchant_id" BIGINT NOT NULL REFERENCES "pos_merchant"."merchants" ("merchant_id") ON DELETE CASCADE,
    "payment_method" VARCHAR(50) NOT NULL,
    "amount" INT NOT NULL,
    "change_amount" INT DEFAULT 0,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_transactions_order_id" ON "pos_transaction"."transactions" ("order_id");
CREATE INDEX IF NOT EXISTS "idx_transactions_merchant_id" ON "pos_transaction"."transactions" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_transactions_status" ON "pos_transaction"."transactions" ("status");
