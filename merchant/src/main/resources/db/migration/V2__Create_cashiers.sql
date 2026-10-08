CREATE SEQUENCE IF NOT EXISTS "cashiers_seq";

CREATE TABLE IF NOT EXISTS "cashiers" (
    "cashier_id" BIGINT NOT NULL DEFAULT nextval('cashiers_seq') PRIMARY KEY,
    "merchant_id" BIGINT NOT NULL REFERENCES "merchants" ("merchant_id") ON DELETE CASCADE,
    "user_id" BIGINT NOT NULL,
    "name" VARCHAR(100) NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_cashiers_merchant_id" ON "cashiers" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_cashiers_user_id" ON "cashiers" ("user_id");
