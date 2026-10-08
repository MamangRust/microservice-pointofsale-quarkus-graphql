CREATE TABLE IF NOT EXISTS "users" (
    "id" BIGINT NOT NULL DEFAULT nextval('users_seq') PRIMARY KEY,
    "firstname" VARCHAR(100) NOT NULL,
    "lastname" VARCHAR(100) NOT NULL,
    "username" VARCHAR(100) UNIQUE NOT NULL,
    "email" VARCHAR(100) UNIQUE NOT NULL,
    "password" VARCHAR(255) NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);


CREATE INDEX IF NOT EXISTS idx_users_email ON "users" ("email");
CREATE INDEX IF NOT EXISTS idx_users_firstname ON "users" ("firstname");
CREATE INDEX IF NOT EXISTS idx_users_lastname ON "users" ("lastname");
CREATE INDEX IF NOT EXISTS idx_users_username ON "users" ("username");
CREATE INDEX IF NOT EXISTS idx_users_created_at ON "users" ("created_at");
