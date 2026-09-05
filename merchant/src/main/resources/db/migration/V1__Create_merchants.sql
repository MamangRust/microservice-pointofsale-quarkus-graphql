CREATE TABLE IF NOT EXISTS "pos_merchant"."merchants" (
    "merchant_id" BIGINT NOT NULL DEFAULT nextval('pos_merchant.merchants_seq') PRIMARY KEY,
    "user_id" BIGINT NOT NULL REFERENCES "pos_identity"."users" ("id") ON DELETE CASCADE,
    "name" VARCHAR(255) NOT NULL,
    "description" TEXT,
    "address" TEXT,
    "contact_email" VARCHAR(100),
    "contact_phone" VARCHAR(20),
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS "idx_merchants_user_id" ON "pos_merchant"."merchants" ("user_id");
CREATE INDEX IF NOT EXISTS "idx_merchants_status" ON "pos_merchant"."merchants" ("status");
CREATE INDEX IF NOT EXISTS "idx_merchants_name" ON "pos_merchant"."merchants" ("name");
