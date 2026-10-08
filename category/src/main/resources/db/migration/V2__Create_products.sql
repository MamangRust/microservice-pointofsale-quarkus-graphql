CREATE SEQUENCE IF NOT EXISTS "products_seq";

CREATE TABLE IF NOT EXISTS "products" (
    "product_id" BIGINT NOT NULL DEFAULT nextval('products_seq') PRIMARY KEY,
    "merchant_id" BIGINT NOT NULL,
    "category_id" BIGINT NOT NULL REFERENCES "categories" ("category_id") ON DELETE CASCADE,
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


CREATE INDEX IF NOT EXISTS "idx_products_merchant_id" ON "products" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_products_category_id" ON "products" ("category_id");
CREATE INDEX IF NOT EXISTS "idx_products_slug" ON "products" ("slug_product");
