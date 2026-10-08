CREATE TABLE IF NOT EXISTS "refresh_tokens" (
    "id" BIGINT NOT NULL DEFAULT nextval('refresh_tokens_seq') PRIMARY KEY,
    "user_id" BIGINT NOT NULL REFERENCES "users" ("id") ON DELETE CASCADE,
    "token" VARCHAR(1000) NOT NULL UNIQUE,
    "expiration" TIMESTAMP NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON "refresh_tokens" ("user_id");
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON "refresh_tokens" ("token");
