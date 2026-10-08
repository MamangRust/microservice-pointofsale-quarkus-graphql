CREATE SEQUENCE IF NOT EXISTS "order_items_seq";

CREATE TABLE IF NOT EXISTS "order_items" (
    "order_item_id" BIGINT NOT NULL DEFAULT nextval('order_items_seq') PRIMARY KEY,
    "order_id" BIGINT NOT NULL REFERENCES "orders" ("order_id") ON DELETE CASCADE,
    "product_id" BIGINT NOT NULL,
    "quantity" INT NOT NULL,
    "price" INT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_order_items_order_id" ON "order_items" ("order_id");
CREATE INDEX IF NOT EXISTS "idx_order_items_product_id" ON "order_items" ("product_id");
