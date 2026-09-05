CREATE TABLE IF NOT EXISTS "pos_identity"."user_roles" (
    "user_id" BIGINT NOT NULL REFERENCES "pos_identity"."users" ("id") ON DELETE CASCADE,
    "role_id" BIGINT NOT NULL REFERENCES "pos_identity"."roles" ("id") ON DELETE CASCADE,
    PRIMARY KEY ("user_id", "role_id")
);
