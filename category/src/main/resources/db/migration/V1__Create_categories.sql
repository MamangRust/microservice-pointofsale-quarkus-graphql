CREATE TABLE IF NOT EXISTS "pos_catalog"."categories" (
    "category_id" BIGINT NOT NULL DEFAULT nextval('pos_catalog.categories_seq') PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL,
    "description" TEXT,
    "slug_category" VARCHAR(255) UNIQUE,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_categories_slug" ON "pos_catalog"."categories" ("slug_category");
