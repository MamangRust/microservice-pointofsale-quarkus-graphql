CREATE TABLE IF NOT EXISTS "orders" (
    "order_id" SERIAL PRIMARY KEY,
    "merchant_id" INT NOT NULL REFERENCES "merchants" ("merchant_id") ON DELETE CASCADE,
    "cashier_id" INT NOT NULL REFERENCES "cashiers" ("cashier_id") ON DELETE CASCADE,
    "total_price" BIGINT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE SEQUENCE IF NOT EXISTS orders_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_orders_merchant_id" ON "orders" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_orders_cashier_id" ON "orders" ("cashier_id");
