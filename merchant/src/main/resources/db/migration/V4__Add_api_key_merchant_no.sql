-- Add api_key and merchant_no columns missing from V1
ALTER TABLE "pos_merchant"."merchants" ADD COLUMN IF NOT EXISTS "merchant_no" UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE "pos_merchant"."merchants" ADD COLUMN IF NOT EXISTS "api_key" VARCHAR(255) NOT NULL DEFAULT '';

-- Create unique indexes
CREATE UNIQUE INDEX IF NOT EXISTS "idx_merchants_merchant_no" ON "pos_merchant"."merchants" ("merchant_no");
CREATE UNIQUE INDEX IF NOT EXISTS "idx_merchants_api_key" ON "pos_merchant"."merchants" ("api_key");
