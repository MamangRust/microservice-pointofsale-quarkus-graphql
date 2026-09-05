CREATE TABLE IF NOT EXISTS "pos_identity"."roles" (
    "id" BIGINT NOT NULL DEFAULT nextval('pos_identity.roles_seq') PRIMARY KEY,
    "role_name" VARCHAR(50) UNIQUE NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS idx_roles_role_name ON "pos_identity"."roles" ("role_name");
CREATE INDEX IF NOT EXISTS idx_roles_created_at ON "pos_identity"."roles" ("created_at");
