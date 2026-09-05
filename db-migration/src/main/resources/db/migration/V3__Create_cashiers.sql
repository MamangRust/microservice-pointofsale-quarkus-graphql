CREATE TABLE IF NOT EXISTS "cashiers" (
    "cashier_id" SERIAL PRIMARY KEY,
    "merchant_id" INT NOT NULL REFERENCES "merchants" ("merchant_id") ON DELETE CASCADE,
    "user_id" INT NOT NULL REFERENCES "users" ("id") ON DELETE CASCADE,
    "name" VARCHAR(100) NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE SEQUENCE IF NOT EXISTS cashiers_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_cashiers_merchant_id" ON "cashiers" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_cashiers_user_id" ON "cashiers" ("user_id");
