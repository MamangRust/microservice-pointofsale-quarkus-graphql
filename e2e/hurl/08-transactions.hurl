# ── 08 · Transactions ───────────────────────────────────────────────────────
# Reads open to ROLE_USER; writes require ROLE_ADMIN/ROLE_STAFF.

GET {{BASE_URL}}/api/transactions?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/active?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# trashed is admin-only — ROLE_USER rejected
GET {{BASE_URL}}/api/transactions/trashed?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

GET {{BASE_URL}}/api/transactions/card-number/CARD-E2E-001?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/merchant/1
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# Write — ROLE_USER rejected
POST {{BASE_URL}}/api/transactions
Authorization: Bearer {{ACCESS_TOKEN}}
Content-Type: application/json
{
  "apiKey": "e2e-key",
  "cardNumber": "CARD-E2E-001",
  "amount": 100000,
  "paymentMethod": "CASH",
  "merchantId": 1,
  "transactionTime": "2026-08-14T00:00:00Z",
  "idempotencyKey": "e2e-tx-1"
}
HTTP 403

# Stats — ROLE_USER rejected
GET {{BASE_URL}}/api/transactions/stats/amount/monthly?year=2026
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403
