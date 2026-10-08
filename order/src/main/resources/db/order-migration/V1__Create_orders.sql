CREATE SEQUENCE IF NOT EXISTS "orders_seq";

CREATE TABLE IF NOT EXISTS "orders" (
    "order_id" BIGINT NOT NULL DEFAULT nextval('orders_seq') PRIMARY KEY,
    "merchant_id" BIGINT NOT NULL,
    "cashier_id" BIGINT NOT NULL,
    "total_price" BIGINT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_orders_merchant_id" ON "orders" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_orders_cashier_id" ON "orders" ("cashier_id");
