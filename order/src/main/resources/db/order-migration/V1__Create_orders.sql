CREATE TABLE IF NOT EXISTS "pos_order"."orders" (
    "order_id" BIGINT NOT NULL DEFAULT nextval('pos_order.orders_seq') PRIMARY KEY,
    "merchant_id" BIGINT NOT NULL REFERENCES "pos_merchant"."merchants" ("merchant_id") ON DELETE CASCADE,
    "cashier_id" BIGINT NOT NULL REFERENCES "pos_merchant"."cashiers" ("cashier_id") ON DELETE CASCADE,
    "total_price" BIGINT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_orders_merchant_id" ON "pos_order"."orders" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_orders_cashier_id" ON "pos_order"."orders" ("cashier_id");
