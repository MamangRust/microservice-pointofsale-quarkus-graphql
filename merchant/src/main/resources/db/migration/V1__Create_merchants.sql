CREATE SEQUENCE IF NOT EXISTS "merchants_seq";

CREATE TABLE IF NOT EXISTS "merchants" (
    "merchant_id" BIGINT NOT NULL DEFAULT nextval('merchants_seq') PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
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


CREATE INDEX IF NOT EXISTS "idx_merchants_user_id" ON "merchants" ("user_id");
CREATE INDEX IF NOT EXISTS "idx_merchants_status" ON "merchants" ("status");
CREATE INDEX IF NOT EXISTS "idx_merchants_name" ON "merchants" ("name");
