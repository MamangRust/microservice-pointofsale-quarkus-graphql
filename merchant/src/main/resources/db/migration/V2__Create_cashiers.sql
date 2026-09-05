CREATE TABLE IF NOT EXISTS "pos_merchant"."cashiers" (
    "cashier_id" BIGINT NOT NULL DEFAULT nextval('pos_merchant.cashiers_seq') PRIMARY KEY,
    "merchant_id" BIGINT NOT NULL REFERENCES "pos_merchant"."merchants" ("merchant_id") ON DELETE CASCADE,
    "user_id" BIGINT NOT NULL REFERENCES "pos_identity"."users" ("id") ON DELETE CASCADE,
    "name" VARCHAR(100) NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_cashiers_merchant_id" ON "pos_merchant"."cashiers" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_cashiers_user_id" ON "pos_merchant"."cashiers" ("user_id");
