# ── 09 · Roles ──────────────────────────────────────────────────────────────
# All role endpoints require ROLE_ADMIN/ROLE_STAFF → ROLE_USER gets 403.

GET {{BASE_URL}}/api/roles?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

GET {{BASE_URL}}/api/roles/active?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

POST {{BASE_URL}}/api/roles
Authorization: Bearer {{ACCESS_TOKEN}}
Content-Type: application/json
{ "name": "ROLE_E2E" }
HTTP 403

GET {{BASE_URL}}/api/roles?page=1&size=5
HTTP 401
