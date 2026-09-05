CREATE TABLE IF NOT EXISTS "pos_order"."order_items" (
    "order_item_id" BIGINT NOT NULL DEFAULT nextval('pos_order.order_items_seq') PRIMARY KEY,
    "order_id" BIGINT NOT NULL REFERENCES "pos_order"."orders" ("order_id") ON DELETE CASCADE,
    "product_id" BIGINT NOT NULL REFERENCES "pos_catalog"."products" ("product_id") ON DELETE CASCADE,
    "quantity" INT NOT NULL,
    "price" INT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_order_items_order_id" ON "pos_order"."order_items" ("order_id");
CREATE INDEX IF NOT EXISTS "idx_order_items_product_id" ON "pos_order"."order_items" ("product_id");
