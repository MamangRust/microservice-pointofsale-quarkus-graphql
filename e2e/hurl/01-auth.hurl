# ── 01 · Auth flow ─────────────────────────────────────────────────────────
# Uses EMAIL / ACCESS_TOKEN / REFRESH_TOKEN already populated by run-e2e.sh
# (register + OTP verify + login happen in the shell harness because the OTP
# must be read from Redis).

# GET /api/auth/me — authenticated (ROLE_USER)
GET {{BASE_URL}}/api/auth/me?userId={{USER_ID}}
Authorization: Bearer {{ACCESS_TOKEN}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.email" == "{{EMAIL}}"

# Refresh token round-trip
POST {{BASE_URL}}/api/auth/refresh
Content-Type: application/json
{
  "refreshToken": "{{REFRESH_TOKEN}}"
}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.accessToken" exists

# 401 without a token
GET {{BASE_URL}}/api/auth/me?userId={{USER_ID}}
HTTP 401

# Bad login
POST {{BASE_URL}}/api/auth/login
Content-Type: application/json
{
  "email": "{{EMAIL}}",
  "password": "wrong-password"
}
HTTP 401
[Asserts]
jsonpath "$.status" == "error"
