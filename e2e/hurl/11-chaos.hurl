# ── 11 · Chaos control plane (Fase 13) ──────────────────────────────────────
# No auth required — gateway-local ChaosManager.

GET {{BASE_URL}}/api/chaos/policies
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{BASE_URL}}/api/chaos/halt
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{BASE_URL}}/api/chaos/reload
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
