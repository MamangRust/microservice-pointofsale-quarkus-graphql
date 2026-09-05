-- Seed default roles for the application
INSERT INTO "pos_identity"."roles" ("role_name", "created_at", "updated_at") VALUES
  ('ROLE_USER',  NOW(), NOW()),
  ('ROLE_ADMIN', NOW(), NOW()),
  ('ROLE_STAFF', NOW(), NOW())
ON CONFLICT DO NOTHING;
