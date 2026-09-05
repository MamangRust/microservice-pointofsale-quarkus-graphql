-- reset_tokens table (previously auto-created by Hibernate strategy=now managed by Flyway)
CREATE TABLE IF NOT EXISTS "pos_identity"."reset_tokens" (
    "id" BIGINT NOT NULL DEFAULT nextval('pos_identity.reset_tokens_seq') PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
    "token" VARCHAR(255) UNIQUE NOT NULL,
    "expiration" TIMESTAMP NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_reset_tokens_user_id ON "pos_identity"."reset_tokens" ("user_id");
CREATE INDEX IF NOT EXISTS idx_reset_tokens_token ON "pos_identity"."reset_tokens" ("token");
