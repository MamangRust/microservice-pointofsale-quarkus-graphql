# ── 99 · Admin happy-path: full CRUD across all domains ─────────────────────
# Uses ADMIN_TOKEN (minted in run-e2e.sh with ROLE_ADMIN).

# ═══ USERS ═══
GET {{BASE_URL}}/api/users?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/users/active?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/users/{{USER_ID}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# Create a second user (admin)
POST {{BASE_URL}}/api/users
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "firstname": "Second",
  "lastname": "User",
  "email": "e2e.second.{{EMAIL}}",
  "password": "Password123!",
  "confirmPassword": "Password123!"
}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"
[Captures]
second_user_id: jsonpath "$.data.id"

# ═══ ROLES ═══
GET {{BASE_URL}}/api/roles?page=1&size=10
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/roles/active?page=1&size=10
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ═══ MERCHANTS ═══
POST {{BASE_URL}}/api/merchants
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "name": "E2E Merchant {{EMAIL}}",
  "userId": {{USER_ID}}
}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.apiKey" exists
[Captures]
merchant_id: jsonpath "$.data.id"
merchant_api_key: jsonpath "$.data.apiKey"

GET {{BASE_URL}}/api/merchants/{{merchant_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{merchant_id}}

GET {{BASE_URL}}/api/merchants?page=1&size=5&search=E2E
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/merchants/active?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/merchants/trashed?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

PUT {{BASE_URL}}/api/merchants/{{merchant_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "merchantId": {{merchant_id}},
  "name": "E2E Merchant Renamed {{EMAIL}}",
  "userId": {{USER_ID}},
  "status": "SUCCESS"
}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ═══ CATEGORIES ═══
POST {{BASE_URL}}/api/categories
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "name": "E2E Category {{EMAIL}}",
  "description": "created by e2e",
  "imageCategory": ""
}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"
[Captures]
category_id: jsonpath "$.data.id"

GET {{BASE_URL}}/api/categories/{{category_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{category_id}}

PUT {{BASE_URL}}/api/categories/{{category_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "categoryId": {{category_id}},
  "name": "E2E Category Renamed {{EMAIL}}",
  "description": "updated",
  "imageCategory": ""
}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/categories/stats/monthly-total-prices?year=2026&month=7
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/categories/stats/yearly-total-prices?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ═══ CASHIERS ═══
POST {{BASE_URL}}/api/cashiers
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "merchantId": {{merchant_id}},
  "userId": {{second_user_id}},
  "name": "E2E Cashier {{EMAIL}}"
}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"
[Captures]
cashier_id: jsonpath "$.data.id"

GET {{BASE_URL}}/api/cashiers/{{cashier_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{cashier_id}}

GET {{BASE_URL}}/api/cashiers/merchant/{{merchant_id}}?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

PUT {{BASE_URL}}/api/cashiers/{{cashier_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "cashierId": {{cashier_id}},
  "name": "E2E Cashier Renamed"
}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/cashiers/stats/monthly-total-sales?year=2026&month=7
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/cashiers/stats/yearly-total-sales?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/cashiers/stats/monthly-total-sales/{{cashier_id}}?year=2026&month=7
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/cashiers/stats/monthly-total-sales/merchant/{{merchant_id}}?year=2026&month=7
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ═══ PRODUCTS ═══
POST {{BASE_URL}}/api/products/create
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "merchantId": {{merchant_id}},
  "categoryId": {{category_id}},
  "name": "E2E Product {{EMAIL}}",
  "description": "created by e2e",
  "price": 150000,
  "countInStock": 25,
  "brand": "E2E",
  "weight": 500,
  "imageProduct": ""
}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"
[Captures]
product_id: jsonpath "$.data.id"

GET {{BASE_URL}}/api/products/{{product_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{product_id}}

GET {{BASE_URL}}/api/products?page=1&size=5&search=E2E
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/products/active?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/products/trashed?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/products/merchant?merchantId={{merchant_id}}&page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/products/category?categoryName=E2E&page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{BASE_URL}}/api/products/update/{{product_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "productId": {{product_id}},
  "merchantId": {{merchant_id}},
  "categoryId": {{category_id}},
  "name": "E2E Product Renamed {{EMAIL}}",
  "description": "updated",
  "price": 175000,
  "countInStock": 20,
  "brand": "E2E",
  "weight": 600,
  "imageProduct": ""
}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ═══ ORDERS ═══
POST {{BASE_URL}}/api/orders
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "merchantId": {{merchant_id}},
  "cashierId": {{cashier_id}},
  "items": [
    { "productId": {{product_id}}, "quantity": 2 }
  ]
}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"
[Captures]
order_id: jsonpath "$.data.id"

GET {{BASE_URL}}/api/orders/{{order_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{order_id}}

GET {{BASE_URL}}/api/orders?page=1&size=5&search=E2E
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/orders/active?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/orders/trashed?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/orders/merchant/{{merchant_id}}?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

PUT {{BASE_URL}}/api/orders/{{order_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "orderId": {{order_id}},
  "cashierId": {{cashier_id}},
  "items": [
    { "orderItemId": 0, "productId": {{product_id}}, "quantity": 3 }
  ]
}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/orders/stats/monthly-total-revenue?year=2026&month=8
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/orders/stats/yearly-total-revenue?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/orders/stats/monthly-total-revenue/{{order_id}}?year=2026&month=8
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/orders/stats/yearly-total-revenue/{{order_id}}?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/orders/stats/monthly-total-revenue/merchant/{{merchant_id}}?year=2026&month=8
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/orders/stats/yearly-total-revenue/merchant/{{merchant_id}}?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ═══ TRANSACTIONS ═══
POST {{BASE_URL}}/api/transactions
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "apiKey": "{{merchant_api_key}}",
  "cardNumber": "{{order_id}}",
  "amount": 600000,
  "paymentMethod": "CASH",
  "merchantId": {{merchant_id}},
  "transactionTime": "2026-08-14T08:00:00Z",
  "idempotencyKey": "e2e-tx-{{EMAIL}}"
}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"
[Captures]
transaction_id: jsonpath "$.data.id"
transaction_no: jsonpath "$.data.transactionNo"

GET {{BASE_URL}}/api/transactions/{{transaction_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{transaction_id}}

# Idempotency: same idempotencyKey must NOT create a second transaction
POST {{BASE_URL}}/api/transactions
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "apiKey": "{{merchant_api_key}}",
  "cardNumber": "{{order_id}}",
  "amount": 600000,
  "paymentMethod": "CASH",
  "merchantId": {{merchant_id}},
  "transactionTime": "2026-08-14T08:00:00Z",
  "idempotencyKey": "e2e-tx-{{EMAIL}}"
}
HTTP 201
[Captures]
transaction_id_2: jsonpath "$.data.id"
[Asserts]
jsonpath "$.data.id" == {{transaction_id}}

GET {{BASE_URL}}/api/transactions?page=1&size=5&search=CARD-E2E
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/active?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/trashed?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/card-number/CARD-E2E-{{EMAIL}}?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/merchant/{{merchant_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# A created transaction is always SUCCESS (insufficient amounts are rejected
# at create), so modifying it is rejected per the domain rule.
PUT {{BASE_URL}}/api/transactions/{{transaction_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "transactionId": {{transaction_id}},
  "apiKey": "{{merchant_api_key}}",
  "cardNumber": "{{order_id}}",
  "amount": 400000,
  "paymentMethod": "QRIS",
  "merchantId": {{merchant_id}},
  "transactionTime": "2026-08-14T09:00:00Z",
  "idempotencyKey": "e2e-tx-{{EMAIL}}"
}
HTTP 400

GET {{BASE_URL}}/api/transactions/stats/amount/monthly?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/stats/amount/yearly?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/stats/amount/monthly/card-number/CARD-E2E-{{EMAIL}}?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/stats/method/monthly?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/stats/method/yearly?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/stats/status/monthly/success?year=2026&month=8
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/stats/status/yearly/success?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/stats/status/monthly/failed?year=2026&month=8
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/transactions/stats/status/yearly/failed?year=2026
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ═══ MERCHANT DOCUMENTS ═══
POST {{BASE_URL}}/api/merchant-documents
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "merchantId": {{merchant_id}},
  "documentType": "LICENSE",
  "documentUrl": "https://example.com/e2e-license.pdf"
}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"
[Captures]
document_id: jsonpath "$.data.documentId"

GET {{BASE_URL}}/api/merchant-documents/{{document_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.data.documentId" == {{document_id}}

GET {{BASE_URL}}/api/merchant-documents?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/merchant-documents/active?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

PUT {{BASE_URL}}/api/merchant-documents/{{document_id}}/status
Authorization: Bearer {{ADMIN_TOKEN}}
Content-Type: application/json
{
  "merchantId": {{merchant_id}},
  "note": "verified by e2e",
  "status": "VERIFIED"
}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ═══ SOFT DELETE + RESTORE (merchant, category, product, order, transaction) ═══
DELETE {{BASE_URL}}/api/merchants/{{merchant_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/merchants/trashed?page=1&size=5&search=E2E
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{BASE_URL}}/api/merchants/{{merchant_id}}/restore
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

DELETE {{BASE_URL}}/api/categories/{{category_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{BASE_URL}}/api/categories/{{category_id}}/restore
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

DELETE {{BASE_URL}}/api/products/{{product_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{BASE_URL}}/api/products/restore/{{product_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

DELETE {{BASE_URL}}/api/orders/{{order_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{BASE_URL}}/api/orders/{{order_id}}/restore
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

DELETE {{BASE_URL}}/api/transactions/{{transaction_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{BASE_URL}}/api/transactions/{{transaction_id}}/restore
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ═══ PERMANENT DELETE (merchant + transaction, cleanup) ═══
# Permanent delete requires the row to be trashed — re-trash the transaction first.
DELETE {{BASE_URL}}/api/transactions/{{transaction_id}}
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

DELETE {{BASE_URL}}/api/transactions/{{transaction_id}}/permanent
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

DELETE {{BASE_URL}}/api/merchants/{{merchant_id}}/permanent
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ═══ ROLE restore-all / trash flows (light checks) ═══
GET {{BASE_URL}}/api/roles/trashed?page=1&size=5
Authorization: Bearer {{ADMIN_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
