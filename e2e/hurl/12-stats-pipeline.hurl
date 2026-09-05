# ─────────────────────────────────────────────────────────────────────────────
# 12-stats-pipeline.hurl — E2E test for the stats pipeline
# Uses ADMIN_TOKEN (stats endpoints require admin role)
# ─────────────────────────────────────────────────────────────────────────────

# ── 1. Verify order stats through gateway ───────────────────────────────────
GET {{BASE_URL}}/api/orders/stats/monthly-total-revenue?year=2024&month=1
Authorization: Bearer {{ADMIN_TOKEN}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ── 2. Verify transaction stats through gateway ─────────────────────────────
GET {{BASE_URL}}/api/transactions/stats/amount/monthly?year=2024
Authorization: Bearer {{ADMIN_TOKEN}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ── 3. Verify cashier stats through gateway ─────────────────────────────────
GET {{BASE_URL}}/api/cashiers/stats/monthly-total-sales?year=2024&month=1
Authorization: Bearer {{ADMIN_TOKEN}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
