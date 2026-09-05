# ── 07 · Orders ─────────────────────────────────────────────────────────────
# Reads open to ROLE_USER; writes require ROLE_ADMIN/ROLE_STAFF.

GET {{BASE_URL}}/api/orders?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/orders/active?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# trashed is admin-only — ROLE_USER rejected
GET {{BASE_URL}}/api/orders/trashed?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

GET {{BASE_URL}}/api/orders/merchant/1?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# Stats — ROLE_USER rejected
GET {{BASE_URL}}/api/orders/stats/monthly-total-revenue?year=2026&month=7
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

# Write — ROLE_USER rejected
POST {{BASE_URL}}/api/orders
Authorization: Bearer {{ACCESS_TOKEN}}
Content-Type: application/json
{
  "merchantId": 1,
  "cashierId": 1,
  "items": [
    { "productId": 1, "quantity": 1 }
  ]
}
HTTP 403
