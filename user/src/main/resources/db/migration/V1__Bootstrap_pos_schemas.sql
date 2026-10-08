-- Identity context sequences. In the 5-instance topology each bounded context
-- owns its own PostgreSQL database (pos_identity), so the sequences that
-- Hibernate/Panache expect (<table>_seq) are created here in the identity DB's
-- public schema instead of a shared cross-context bootstrap.
CREATE SEQUENCE IF NOT EXISTS "users_seq";
CREATE SEQUENCE IF NOT EXISTS "roles_seq";
CREATE SEQUENCE IF NOT EXISTS "refresh_tokens_seq";
CREATE SEQUENCE IF NOT EXISTS "reset_tokens_seq";
