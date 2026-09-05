# ── 06 · Products ───────────────────────────────────────────────────────────
# Reads open to ROLE_USER; writes require ROLE_ADMIN/ROLE_STAFF.

GET {{BASE_URL}}/api/products?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/products/active?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/products/merchant?merchantId=1&page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{BASE_URL}}/api/products/category?categoryName=Electronics&page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# Write — ROLE_USER rejected
POST {{BASE_URL}}/api/products/create
Authorization: Bearer {{ACCESS_TOKEN}}
Content-Type: application/json
{
  "merchantId": 1,
  "categoryId": 1,
  "name": "E2E Product",
  "description": "created by e2e",
  "price": 1000,
  "countInStock": 10,
  "brand": "E2E",
  "weight": 100,
  "imageProduct": ""
}
HTTP 403

# Anonymous → 401
GET {{BASE_URL}}/api/products?page=1&size=5
HTTP 401
