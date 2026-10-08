CREATE TABLE IF NOT EXISTS "roles" (
    "id" BIGINT NOT NULL DEFAULT nextval('roles_seq') PRIMARY KEY,
    "role_name" VARCHAR(50) UNIQUE NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS idx_roles_role_name ON "roles" ("role_name");
CREATE INDEX IF NOT EXISTS idx_roles_created_at ON "roles" ("created_at");
