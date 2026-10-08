CREATE SEQUENCE IF NOT EXISTS "categories_seq";

CREATE TABLE IF NOT EXISTS "categories" (
    "category_id" BIGINT NOT NULL DEFAULT nextval('categories_seq') PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL,
    "description" TEXT,
    "slug_category" VARCHAR(255) UNIQUE,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_categories_slug" ON "categories" ("slug_category");
