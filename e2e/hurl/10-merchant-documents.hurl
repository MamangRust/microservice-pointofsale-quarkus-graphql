# ── 10 · Merchant Documents ─────────────────────────────────────────────────
# All endpoints require ROLE_ADMIN/ROLE_STAFF → ROLE_USER gets 403.

GET {{BASE_URL}}/api/merchant-documents?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

GET {{BASE_URL}}/api/merchant-documents/active?page=1&size=5
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 403

POST {{BASE_URL}}/api/merchant-documents
Authorization: Bearer {{ACCESS_TOKEN}}
Content-Type: application/json
{
  "merchantId": 1,
  "documentType": "LICENSE",
  "documentUrl": "https://example.com/doc.pdf"
}
HTTP 403

GET {{BASE_URL}}/api/merchant-documents?page=1&size=5
HTTP 401
