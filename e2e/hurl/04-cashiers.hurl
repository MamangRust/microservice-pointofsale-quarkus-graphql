# ── 04 · Cashiers ───────────────────────────────────────────────────────────
# All cashier endpoints require ROLE_ADMIN/ROLE_STAFF → ROLE_USER gets 403.

GET {{BASE_URL}}/api/cashiers?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

GET {{BASE_URL}}/api/cashiers/active?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

POST {{BASE_URL}}/api/cashiers
Authorization: Bearer {{ACCESS_TOKEN}}
Content-Type: application/json
{
  "merchantId": 1,
  "userId": {{USER_ID}},
  "name": "E2E Cashier"
}
HTTP 403

GET {{BASE_URL}}/api/cashiers/stats/monthly-total-sales?year=2026&month=7
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403
