#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# E2E runner for the Quarkus Point-of-Sale gateway (hurl).
#
# Prereqs:
#   - Stack running:  docker compose -f deployments/local/docker-compose.yml up -d
#   - hurl >= 4.x     (hurl --version)
#   - redis-cli reachable inside the redis-node-1 container for OTP extraction
#
# Flow:
#   1. setup: register a unique user -> grab OTP from Redis -> verify -> login
#      -> write auth tokens + ids into e2e/vars.env
#   2. run every *.hurl file in e2e/hurl/ (alphabetical order)
#
# Usage:  ./e2e/run-e2e.sh [--base-url http://localhost:5000]
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

cd "$(dirname "$0")"

BASE_URL="${BASE_URL:-http://localhost:5000}"
VARS_FILE="$(pwd)/vars.env"
HURL_DIR="$(pwd)/hurl"
TS="$(date +%s)"
EMAIL="e2e.user.${TS}@test.local"

rm -f "$VARS_FILE"

echo "==> Base URL   : $BASE_URL"
echo "==> Test email : $EMAIL"

# ── 1. Register ──────────────────────────────────────────────────────────────
echo "==> Registering user..."
REG=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"firstname\":\"E2E\",\"lastname\":\"Runner\",\"email\":\"$EMAIL\",\"password\":\"Password123!\",\"confirmPassword\":\"Password123!\"}" \
) || { echo "REGISTER FAILED"; curl -s -X POST "$BASE_URL/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"firstname\":\"E2E\",\"lastname\":\"Runner\",\"email\":\"$EMAIL\",\"password\":\"Password123!\",\"confirmPassword\":\"Password123!\"}"; exit 1; }
echo "$REG" | head -c 300; echo
# user id from register response
USER_ID=$(echo "$REG" | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d.get("data",{}).get("id",""))' 2>/dev/null || echo "")

# ── 2. Grab OTP from Redis (standalone or cluster) ──────────────────────────
echo "==> Fetching OTP from Redis..."
OTP=""
# Try standalone redis first
OTP=$(docker exec pos-redis-standalone redis-cli -a dragon_knight --scan --pattern "verification_code:*" 2>/dev/null \
      | head -1 | sed 's/verification_code://')
# If not found, try cluster nodes
if [ -z "$OTP" ]; then
  for i in 1 2 3; do
    OTP=$(docker exec redis_node_$i redis-cli -a dragon_knight -c --scan --pattern "verification_code:*" 2>/dev/null \
          | head -1 | sed 's/verification_code://') 
    if [ -n "$OTP" ]; then break; fi
  done
fi
if [ -z "$OTP" ]; then
  echo "!! OTP not found in Redis — dump keys:"
  for i in 1 2 3 4 5 6; do docker exec redis_node_$i redis-cli -a dragon_knight -c --scan --pattern "verification*" 2>/dev/null; done | sort -u | head
  exit 1
fi
echo "==> OTP: $OTP"

# ── 3. Verify ────────────────────────────────────────────────────────────────
echo "==> Verifying email..."
curl -sf -X POST "$BASE_URL/api/auth/verify" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"$OTP\"}" | head -c 200; echo

# ── 4. Login ─────────────────────────────────────────────────────────────────
echo "==> Logging in..."
LOGIN=$(curl -sf -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Password123!\"}")
echo "$LOGIN" | head -c 300; echo
ACCESS_TOKEN=$(echo "$LOGIN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')
REFRESH_TOKEN=$(echo "$LOGIN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["refreshToken"])')
[ -z "$USER_ID" ] && USER_ID=""

# ── 4b. Mint an admin token (login always issues ROLE_USER; admin-gated
# endpoints need a token carrying ROLE_ADMIN signed with the same key).
KEY="$(pwd)/../auth/src/main/resources/privateKey.pem"
if [ -f "$KEY" ]; then
  NOW=$(date +%s)
  EXP=$((NOW + 3600))
  HDR=$(printf '{"alg":"RS256","typ":"JWT"}' | base64 -w0 | tr '+/' '-_' | tr -d '=')
  PAY=$(printf '{"iss":"https://example-quarkus-opentelemetry.com","sub":"%s","userId":%s,"iat":%s,"exp":%s,"groups":["ROLE_ADMIN","user"]}' "$EMAIL" "${USER_ID:-1}" "$NOW" "$EXP" | base64 -w0 | tr '+/' '-_' | tr -d '=')
  SIG=$(printf '%s.%s' "$HDR" "$PAY" | openssl dgst -sha256 -sign "$KEY" -binary | base64 -w0 | tr '+/' '-_' | tr -d '=')
  ADMIN_TOKEN="$HDR.$PAY.$SIG"
  echo "==> Admin token minted (ROLE_ADMIN)"
else
  ADMIN_TOKEN=""
  echo "!! privateKey.pem not found — admin suite skipped"
fi

# ── 5. Write vars ────────────────────────────────────────────────────────────
{
  echo "BASE_URL=$BASE_URL"
  echo "EMAIL=$EMAIL"
  echo "ACCESS_TOKEN=$ACCESS_TOKEN"
  echo "REFRESH_TOKEN=$REFRESH_TOKEN"
  echo "ADMIN_TOKEN=$ADMIN_TOKEN"
  echo "USER_ID=$USER_ID"
  echo "OTP=$OTP"
} > "$VARS_FILE"

echo "==> Vars written to $VARS_FILE"
echo "==> Tokens: access=${ACCESS_TOKEN:0:20}... refresh=${REFRESH_TOKEN:0:20}..."

# ── 6. Run hurl files ────────────────────────────────────────────────────────
FAILED=0
for f in "$HURL_DIR"/*.hurl; do
  name="$(basename "$f")"
  echo ""
  echo "═══════════════════════════════════════════════════════════════"
  echo "==> Running: $name"
  echo "═══════════════════════════════════════════════════════════════"
  if ! hurl --test --variables-file "$VARS_FILE" --variable "BASE_URL=$BASE_URL" "$f"; then
    echo "!! FAILED: $name"
    FAILED=$((FAILED+1))
  fi
done

echo ""
echo "───────────────────────────────────────────────────────────────"
if [ "$FAILED" -eq 0 ]; then
  echo "ALL HURL TESTS PASSED ✔"
else
  echo "$FAILED hurl file(s) FAILED ✘"
fi
exit $FAILED
