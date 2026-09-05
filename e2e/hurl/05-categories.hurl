# ── 05 · Categories ─────────────────────────────────────────────────────────
# Read endpoints are open to ROLE_USER; writes require ROLE_ADMIN/ROLE_STAFF.

# Reads — allowed with ROLE_USER token
GET {{BASE_URL}}/api/categories?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/categories/active?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# trashed is admin-only — ROLE_USER rejected
GET {{BASE_URL}}/api/categories/trashed?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

# Writes — ROLE_USER rejected
POST {{BASE_URL}}/api/categories
Authorization: Bearer {{ACCESS_TOKEN}}
Content-Type: application/json
{
  "name": "E2E Category",
  "description": "created by e2e",
  "imageCategory": ""
}
HTTP 403

# Anonymous reads — 401
GET {{BASE_URL}}/api/categories?page=1&size=5
HTTP 401
