# ── 02 · Users ──────────────────────────────────────────────────────────────
# NOTE: role-gated endpoints require ROLE_ADMIN/ROLE_STAFF. The login-issued
# token only carries ROLE_USER, so admin-only calls (list/create/delete) are
# expected to be rejected (403). The happy-path admin flow is covered by the
# dedicated admin token suite (99-admin.hurl).

# List users — ROLE_USER is NOT allowed here (403 expected)
GET {{BASE_URL}}/api/users?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

# Get own user by id — needs ROLE_ADMIN/ROLE_STAFF too
GET {{BASE_URL}}/api/users/{{USER_ID}}
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

# Unauthenticated → 401
GET {{BASE_URL}}/api/users?page=1&size=5
HTTP 401
