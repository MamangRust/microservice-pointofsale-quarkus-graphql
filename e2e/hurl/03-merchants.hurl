# ── 03 · Merchants ──────────────────────────────────────────────────────────
# All merchant management endpoints require ROLE_ADMIN/ROLE_STAFF.
# ROLE_USER token → 403; anonymous → 401.

GET {{BASE_URL}}/api/merchants?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

GET {{BASE_URL}}/api/merchants/1
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

POST {{BASE_URL}}/api/merchants
Authorization: Bearer {{ACCESS_TOKEN}}
Content-Type: application/json
{
  "name": "E2E Merchant",
  "userId": {{USER_ID}}
}
HTTP 403

GET {{BASE_URL}}/api/merchants/active?page=1&size=5
HTTP 401
