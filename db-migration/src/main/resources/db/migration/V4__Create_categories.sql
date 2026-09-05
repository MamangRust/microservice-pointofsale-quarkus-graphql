CREATE TABLE IF NOT EXISTS "categories" (
    "category_id" SERIAL PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL,
    "description" TEXT,
    "slug_category" VARCHAR(255) UNIQUE,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE SEQUENCE IF NOT EXISTS categories_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_categories_slug" ON "categories" ("slug_category");
