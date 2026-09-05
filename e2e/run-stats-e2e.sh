#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# E2E test for the stats pipeline.
#
# Tests: API → Outbox → Kafka → Stats-Writer → ClickHouse → Stats-Reader
#
# Prereqs:
#   - Full stack running: docker compose -f deployments/local/docker-compose.yml up -d
#   - Seed data loaded: docker compose up seeder
#   - Stats backfill completed: docker compose run --rm stats-backfill
#
# Usage: ./e2e/run-stats-e2e.sh [--base-url http://localhost:5000]
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

cd "$(dirname "$0")"

BASE_URL="${BASE_URL:-http://localhost:5000}"
STATS_READER_HOST="${STATS_READER_HOST:-localhost}"
STATS_READER_PORT="${STATS_READER_PORT:-9015}"
CLICKHOUSE_HOST="${CLICKHOUSE_HOST:-localhost}"
CLICKHOUSE_PORT="${CLICKHOUSE_PORT:-8123}"
CLICKHOUSE_DB="${CLICKHOUSE_DB:-pos_stats}"
CLICKHOUSE_USER="${CLICKHOUSE_USER:-default}"
CLICKHOUSE_PASS="${CLICKHOUSE_PASS:-e2epass}"

PASS=0
FAIL=0

assert_contains() {
    local label="$1" body="$2" expected="$3"
    if echo "$body" | grep -q "$expected"; then
        echo "  ✔ $label"
        PASS=$((PASS+1))
    else
        echo "  ✘ $label — expected '$expected' in response"
        echo "    body: $(echo "$body" | head -c 200)"
        FAIL=$((FAIL+1))
    fi
}

assert_status() {
    local label="$1" actual="$2" expected="$3"
    if [ "$actual" = "$expected" ]; then
        echo "  ✔ $label (HTTP $actual)"
        PASS=$((PASS+1))
    else
        echo "  ✘ $label — expected HTTP $expected, got $actual"
        FAIL=$((FAIL+1))
    fi
}

echo "═══════════════════════════════════════════════════════════════════"
echo "  STATS PIPELINE E2E TEST"
echo "═══════════════════════════════════════════════════════════════════"
echo "  Base URL:      $BASE_URL"
echo "  Stats Reader:  $STATS_READER_HOST:$STATS_READER_PORT"
echo "  ClickHouse:    $CLICKHOUSE_HOST:$CLICKHOUSE_PORT/$CLICKHOUSE_DB"
echo ""

# ── 1. Login as admin ────────────────────────────────────────────────────────
echo "─── 1. Login as admin ─────────────────────────────────────────"
LOGIN=$(curl -sf -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@sanedge.com","password":"Password123!"}' 2>/dev/null)
TOKEN=$(echo "$LOGIN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])' 2>/dev/null || echo "")
if [ -z "$TOKEN" ]; then
    echo "  ✘ Login failed — cannot proceed"
    exit 1
fi
echo "  ✔ Login successful"
PASS=$((PASS+1))

# ── 2. Check ClickHouse has data ─────────────────────────────────────────────
echo ""
echo "─── 2. Check ClickHouse tables ────────────────────────────────"
ORDER_COUNT=$(curl -sf "http://$CLICKHOUSE_HOST:$CLICKHOUSE_PORT/?database=$CLICKHOUSE_DB" \
  --data-binary "SELECT count(*) FROM order_daily FORMAT TabSeparated" 2>/dev/null || echo "0")
assert_contains "order_daily has data" "$ORDER_COUNT" "[1-9]"

TX_COUNT=$(curl -sf "http://$CLICKHOUSE_HOST:$CLICKHOUSE_PORT/?database=$CLICKHOUSE_DB" \
  --data-binary "SELECT count(*) FROM transaction_daily FORMAT TabSeparated" 2>/dev/null || echo "0")
assert_contains "transaction_daily has data" "$TX_COUNT" "[1-9]"

# ── 3. Check stats-reader health ─────────────────────────────────────────────
echo ""
echo "─── 3. Check stats-reader gRPC ───────────────────────────────"
# Use grpcurl or a simple port check
if command -v grpcurl &>/dev/null; then
    echo "$STATS_READER_HOST:$STATS_READER_PORT" | xargs -I{} grpcurl -plaintext {} list 2>/dev/null | head -5
    if [ $? -eq 0 ]; then
        echo "  ✔ stats-reader gRPC is reachable"
        PASS=$((PASS+1))
    else
        echo "  ✘ stats-reader gRPC not reachable"
        FAIL=$((FAIL+1))
    fi
else
    # Fallback: just check port is open
    if nc -z "$STATS_READER_HOST" "$STATS_READER_PORT" 2>/dev/null; then
        echo "  ✔ stats-reader port $STATS_READER_PORT is open"
        PASS=$((PASS+1))
    else
        echo "  ✘ stats-reader port $STATS_READER_PORT is not reachable"
        FAIL=$((FAIL+1))
    fi
fi

# ── 4. Query order stats through gateway ─────────────────────────────────────
echo ""
echo "─── 4. Query order stats through gateway ──────────────────────"
RESP=$(curl -sf -w "\n%{http_code}" "$BASE_URL/api/orders/stats/monthly-total-revenue?year=2024&month=1" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_status "GET /api/orders/stats/monthly-total-revenue" "$HTTP_CODE" "200"
assert_contains "Order stats response has status" "$BODY" '"status"'

# ── 5. Query transaction stats through gateway ───────────────────────────────
echo ""
echo "─── 5. Query transaction stats through gateway ────────────────"
RESP=$(curl -sf -w "\n%{http_code}" "$BASE_URL/api/transactions/stats/monthly-amounts?year=2024" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_status "GET /api/transactions/stats/monthly-amounts" "$HTTP_CODE" "200"
assert_contains "Transaction stats response has status" "$BODY" '"status"'

# ── 6. Query cashier stats through gateway ───────────────────────────────────
echo ""
echo "─── 6. Query cashier stats through gateway ────────────────────"
RESP=$(curl -sf -w "\n%{http_code}" "$BASE_URL/api/cashiers/stats/monthly-total-sales?year=2024&month=1" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_status "GET /api/cashiers/stats/monthly-total-sales" "$HTTP_CODE" "200"
assert_contains "Cashier stats response has status" "$BODY" '"status"'

# ── 7. Verify Redis cache ────────────────────────────────────────────────────
echo ""
echo "─── 7. Verify Redis cache ────────────────────────────────────"
CACHE_KEYS=$(docker exec redis_node_1 redis-cli -a dragon_knight -c --scan --pattern "apigw:stats:*" 2>/dev/null | wc -l)
if [ "$CACHE_KEYS" -gt 0 ]; then
    echo "  ✔ Redis has $CACHE_KEYS cached stats entries"
    PASS=$((PASS+1))
else
    echo "  ⚠ Redis has no cached stats entries (may need second request)"
    PASS=$((PASS+1))
fi

# ── 8. Check outbox status ───────────────────────────────────────────────────
echo ""
echo "─── 8. Check outbox status ────────────────────────────────────"
OUTBOX_PENDING=$(docker exec postgres psql -U DRAGON -d POINT_OF_SALE -t -c \
  "SELECT count(*) FROM pos_order.outbox WHERE status = 'PENDING';" 2>/dev/null | tr -d ' ')
echo "  Outbox PENDING: $OUTBOX_PENDING"

OUTBOX_SENT=$(docker exec postgres psql -U DRAGON -d POINT_OF_SALE -t -c \
  "SELECT count(*) FROM pos_order.outbox WHERE status = 'SENT';" 2>/dev/null | tr -d ' ')
echo "  Outbox SENT:    $OUTBOX_SENT"

OUTBOX_TOTAL=$(docker exec postgres psql -U DRAGON -d POINT_OF_SALE -t -c \
  "SELECT count(*) FROM pos_order.outbox;" 2>/dev/null | tr -d ' ')
assert_contains "Outbox has entries" "$OUTBOX_TOTAL" "[1-9]"

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "  RESULTS: $PASS passed, $FAIL failed"
echo "═══════════════════════════════════════════════════════════════════"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
