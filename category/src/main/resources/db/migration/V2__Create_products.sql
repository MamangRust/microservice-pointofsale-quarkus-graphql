CREATE TABLE IF NOT EXISTS "pos_catalog"."products" (
    "product_id" BIGINT NOT NULL DEFAULT nextval('pos_catalog.products_seq') PRIMARY KEY,
    "merchant_id" BIGINT NOT NULL REFERENCES "pos_merchant"."merchants" ("merchant_id") ON DELETE CASCADE,
    "category_id" BIGINT NOT NULL REFERENCES "pos_catalog"."categories" ("category_id") ON DELETE CASCADE,
    "name" VARCHAR(255) NOT NULL,
    "description" TEXT,
    "price" INT NOT NULL,
    "count_in_stock" INT NOT NULL DEFAULT 0,
    "brand" VARCHAR(255),
    "weight" INT,
    "slug_product" VARCHAR(255) UNIQUE,
    "image_product" VARCHAR(255),
    "barcode" VARCHAR(255) UNIQUE,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_products_merchant_id" ON "pos_catalog"."products" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_products_category_id" ON "pos_catalog"."products" ("category_id");
CREATE INDEX IF NOT EXISTS "idx_products_slug" ON "pos_catalog"."products" ("slug_product");
