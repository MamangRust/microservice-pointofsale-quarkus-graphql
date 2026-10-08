CREATE TABLE IF NOT EXISTS "user_roles" (
    "user_id" BIGINT NOT NULL REFERENCES "users" ("id") ON DELETE CASCADE,
    "role_id" BIGINT NOT NULL REFERENCES "roles" ("id") ON DELETE CASCADE,
    PRIMARY KEY ("user_id", "role_id")
);
