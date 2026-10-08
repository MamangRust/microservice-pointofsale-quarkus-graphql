#!/usr/bin/env bash
# F4 e2e verification — stats-reader + proto stats + endpoint stats + backfill.
# Fresh DB → services+stats_reader → backfill → cross-check OLTP vs CH → hurl
# (regresi F1) → 7 endpoint stats via apigateway → cross-check akhir.
set -euo pipefail
ROOT="/home/hoover/Projects/golang/go/echo/Untitled_Folder/monolith-pointofsale-grpc"
cd "$ROOT"

export KAFKA_BROKERS=localhost:29092 CLICKHOUSE_ADDR=localhost:9000 CLICKHOUSE_DATABASE=pos

# port -> binary
declare -A PORT_BIN=(
    [50051]=auth [50052]=role [50053]=user [50054]=category [50055]=cashier
    [50056]=merchant [50057]=order_item [50058]=order [50059]=product
    [50060]=transaction [50070]=stats_reader
)
SERVICE_ORDER=(auth role user category cashier merchant order_item order product transaction apigateway)

cleanup() {
    for pid in "${pids[@]:-}"; do kill -9 "$pid" 2>/dev/null || true; done
    pkill -9 -f 'bin/(auth|role|user|category|cashier|merchant|order_item|order|product|transaction|apigateway|stats_writer|stats_reader)' 2>/dev/null || true
    sleep 1
}
trap cleanup EXIT

port_open() { timeout 1 bash -c "</dev/tcp/127.0.0.1/$1" 2>/dev/null; }

wait_all_ports() {
    for _ in $(seq 1 60); do
        ok=1
        for p in "${!PORT_BIN[@]}"; do
            if ! port_open "$p"; then ok=0; break; fi
        done
        if [ "$ok" -eq 1 ] && curl -sf "http://localhost:5000/api/auth/hello" >/dev/null 2>&1; then return 0; fi
        sleep 2
    done
    return 1
}

echo "==> [0] Pastikan infra POS jalan (postgres + redis + kafka + clickhouse)"
for c in postgres redis_pointofsale my-kafka-pointofsale clickhouse; do
    if ! docker ps --format '{{.Names}}' | grep -q "^$c$"; then
        if docker ps -a --format '{{.Names}}' | grep -q "^$c$"; then
            docker start "$c" >/dev/null 2>&1
        else
            docker compose -f deployments/local/docker-compose.infra.yml up -d redis kafka >/dev/null 2>&1
        fi
    fi
done
sleep 8
for c in postgres redis_pointofsale my-kafka-pointofsale clickhouse; do
    docker ps --format '{{.Names}}' | grep -q "^$c$" || { echo "❌ infra $c tidak jalan"; exit 1; }
done
docker exec postgres psql -U DRAGON -d POINTOFSALE -c "SELECT 1" >/dev/null 2>&1 || { echo "❌ postgres tidak siap"; exit 1; }
docker exec clickhouse clickhouse-client --query "SELECT 1" >/dev/null 2>&1 || { echo "❌ clickhouse tidak siap"; exit 1; }
echo "infra ok"

echo "==> [1] Reset DB (PG + ClickHouse pos) → migrate → seeder"
docker exec postgres psql -U DRAGON -d POINTOFSALE -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;" >/dev/null
docker exec clickhouse clickhouse-client --query "DROP DATABASE IF EXISTS pos" 2>/dev/null || true
go run service/migrate/main.go up 2>&1 | tail -2
go run service/seeder/main.go 2>&1 | tail -2

echo "==> [2] Build semua service + stats_writer + stats_reader"
mkdir -p bin
for s in "${SERVICE_ORDER[@]}"; do
    if [ -f "service/$s/cmd/main.go" ]; then
        (cd "service/$s" && go build -o "$ROOT/bin/$s" ./cmd/main.go)
    else
        (cd "service/$s" && go build -o "$ROOT/bin/$s" ./main.go)
    fi
done
(cd service/stats_writer && go build -o "$ROOT/bin/stats_writer" ./cmd/main.go)
(cd service/stats_reader && go build -o "$ROOT/bin/stats_reader" ./cmd/main.go)
echo "build ok"

echo "==> [3] Start service lokal + stats_writer + stats_reader"
cleanup
sleep 2
mkdir -p logs
set -a
# shellcheck disable=SC1091
source "$ROOT/.env"
set +a
# MinConns default 50/service → ~500 koneksi outbound saat semua service start.
# Port gRPC (50051-50070) ada DI DALAM ephemeral range kernel (32768-60999),
# jadi koneksi outbound bisa kebetulan dapat source port = port gRPC → TIME_WAIT
# memblokir bind berikutnya. Kecilkan pool untuk uji e2e.
export KAFKA_BROKERS=localhost:29092 APP_ENV=development DB_MIN_IDLE_CONNS=1 DB_MAX_OPEN_CONNS=20
pids=()
for s in "${SERVICE_ORDER[@]}" stats_writer stats_reader; do
    echo "  → $s"
    nohup "./bin/$s" >> "logs/$s.log" 2>&1 &
    pids+=("$!")
done

echo "==> [4] Tunggu gRPC ports; restart service yang gagal bind (tunggu TIME_WAIT 65s)"
round=1
while [ "$round" -le 3 ]; do
    if wait_all_ports; then break; fi
    echo "  ronde $round: beberapa port belum siap — tunggu TIME_WAIT kadaluarsa (65s) lalu restart"
    sleep 65
    for p in "${!PORT_BIN[@]}"; do
        if ! port_open "$p"; then
            b="${PORT_BIN[$p]}"
            echo "  → restart $b ($p)"
            nohup "./bin/$b" >> "logs/$b.log" 2>&1 &
            pids+=("$!")
        fi
    done
    round=$((round + 1))
done
for p in "${!PORT_BIN[@]}"; do
    port_open "$p" || { echo "❌ gRPC port $p tidak siap"; ss -tlnp 2>/dev/null | grep ":$p " || true; exit 1; }
done
curl -sf "http://localhost:5000/api/auth/hello" >/dev/null 2>&1 || { echo "❌ apigateway tidak siap"; exit 1; }
echo "services ready"

echo "==> [5] Backfill OLTP → ClickHouse (bootstrap stats)"
go run service/stats_writer/cmd/main.go backfill 2>&1 | tail -3

echo "==> [5b] Cross-check OLTP vs CH (sebelum hurl — harus identik)"
YEAR0=$(date +%Y)
docker exec postgres psql -U DRAGON -d POINTOFSALE -t -A -c "SELECT 'OLTP orders total', COALESCE(SUM(total_price),0) FROM orders WHERE deleted_at IS NULL AND EXTRACT(YEAR FROM created_at)=$YEAR0;" 2>/dev/null || true
docker exec clickhouse clickhouse-client --query "SELECT 'CH orders total', sum(total_price) FROM pos.order_daily WHERE toYear(event_time)=$YEAR0" 2>&1 || true
docker exec postgres psql -U DRAGON -d POINTOFSALE -t -A -c "SELECT 'OLTP txns total', COALESCE(SUM(amount),0) FROM transactions WHERE deleted_at IS NULL AND payment_status IN ('success','Completed') AND EXTRACT(YEAR FROM created_at)=$YEAR0;" 2>/dev/null || true
docker exec clickhouse clickhouse-client --query "SELECT 'CH txns total', sumIf(amount, lower(status) IN ('success','completed')) FROM pos.transaction_daily WHERE toYear(event_time)=$YEAR0" 2>&1 || true

echo "==> [6] Jalankan hurl suite (regresi F1)"
cd "$ROOT/tests/hurl"
UUID_VAL="$(uuidgen)"
PASS=0; FAIL=0; FAILED=()
for f in $(ls *.hurl | grep -v '^trace_smoke\.hurl$'); do
    echo "  → $f"
    if hurl --variable baseUrl="http://localhost:5000" --variable uuid="$UUID_VAL" --test "$f" >/dev/null 2>&1; then
        PASS=$((PASS + 1)); echo "  ✅ PASS $f"
    else
        FAIL=$((FAIL + 1)); FAILED+=("$f"); echo "  ❌ FAIL $f"
    fi
    sleep 2
done
echo "== HURL RESULT == PASS: $PASS FAIL: $FAIL"
[ "$FAIL" -eq 0 ] || { echo "HURL GAGAL: ${FAILED[*]}"; exit 1; }

echo "==> [7] Tunggu stats-writer flush (~12s)"
sleep 12
tail -3 "$ROOT/logs/stats_writer.log" 2>/dev/null || true
echo "--- ClickHouse rows (pos DB) ---"
docker exec clickhouse clickhouse-client --query "SELECT 'order_daily', count() FROM pos.order_daily UNION ALL SELECT 'order_item_daily', count() FROM pos.order_item_daily UNION ALL SELECT 'transaction_daily', count() FROM pos.transaction_daily" 2>&1 || true

echo "==> [8] Verifikasi endpoint stats (register → login → 7 endpoint)"
cd "$ROOT"
STATS_UUID="$(uuidgen | tr -d '-' | cut -c1-12)"
curl -s -X POST http://localhost:5000/api/auth/register -H 'Content-Type: application/json' \
  -d "{\"firstname\":\"Stats\",\"lastname\":\"Tester\",\"email\":\"stats.$STATS_UUID@example.com\",\"password\":\"password123\",\"confirm_password\":\"password123\"}" >/dev/null 2>&1 || true
TOKEN=$(curl -s -X POST http://localhost:5000/api/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"stats.$STATS_UUID@example.com\",\"password\":\"password123\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("data",{}).get("access_token",""))' 2>/dev/null)
if [ -z "$TOKEN" ]; then
    TOKEN=$(curl -s -X POST http://localhost:5000/api/auth/login -H 'Content-Type: application/json' \
      -d "{\"email\":\"stats.$STATS_UUID@example.com\",\"password\":\"password123\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("data",{}).get("token",""))' 2>/dev/null)
fi
[ -n "$TOKEN" ] || { echo "❌ login gagal"; exit 1; }
echo "token OK (${#TOKEN} chars)"

YEAR=$(date +%Y)
MONTH=$(date +%-m)
CASHIER_ID=$(docker exec postgres psql -U DRAGON -d POINTOFSALE -t -A -c "SELECT cashier_id FROM orders WHERE deleted_at IS NULL AND cashier_id > 0 ORDER BY order_id LIMIT 1;" 2>/dev/null || echo 1)
[ -n "$CASHIER_ID" ] || CASHIER_ID=1
echo "params: year=$YEAR month=$MONTH cashier_id=$CASHIER_ID"

stats_call() {
    local name=$1 url=$2
    echo "--- $name ---"
    curl -s "$url" -H "Authorization: Bearer $TOKEN"; echo
}
stats_call "/api/order-stats/monthly-total-revenue" "http://localhost:5000/api/order-stats/monthly-total-revenue?year=$YEAR&month=$MONTH"
stats_call "/api/order-stats/yearly-revenue" "http://localhost:5000/api/order-stats/yearly-revenue?year=$YEAR"
stats_call "/api/order-stats/cashier/monthly-revenue?cashier_id=$CASHIER_ID" "http://localhost:5000/api/order-stats/cashier/monthly-revenue?cashier_id=$CASHIER_ID"
stats_call "/api/product-stats/monthly-sold" "http://localhost:5000/api/product-stats/monthly-sold?year=$YEAR&month=$MONTH"
stats_call "/api/category-stats/monthly-sold" "http://localhost:5000/api/category-stats/monthly-sold?year=$YEAR&month=$MONTH"
stats_call "/api/transaction-stats/monthly-success" "http://localhost:5000/api/transaction-stats/monthly-success?year=$YEAR&month=$MONTH"
stats_call "/api/cashier-stats/monthly-orders?cashier_id=$CASHIER_ID" "http://localhost:5000/api/cashier-stats/monthly-orders?cashier_id=$CASHIER_ID"

echo "==> [9] Cross-check vs OLTP (sampel)"
echo "--- OLTP: orders sum total_price tahun $YEAR ---"
docker exec postgres psql -U DRAGON -d POINTOFSALE -t -A -c "SELECT COALESCE(SUM(total_price),0) FROM orders WHERE deleted_at IS NULL AND EXTRACT(YEAR FROM created_at)=$YEAR;" 2>/dev/null || true
echo "--- CH: order_daily sum total_price tahun $YEAR ---"
docker exec clickhouse clickhouse-client --query "SELECT sum(total_price) FROM pos.order_daily WHERE toYear(event_time)=$YEAR" 2>&1 || true
echo "--- OLTP: transactions sukses bulan $MONTH ---"
docker exec postgres psql -U DRAGON -d POINTOFSALE -t -A -c "SELECT COALESCE(SUM(amount),0), COUNT(*) FROM transactions WHERE deleted_at IS NULL AND payment_status IN ('success','Completed') AND EXTRACT(YEAR FROM created_at)=$YEAR AND EXTRACT(MONTH FROM created_at)=$MONTH;" 2>/dev/null || true
echo "--- CH: transaction_daily sukses bulan $MONTH ---"
docker exec clickhouse clickhouse-client --query "SELECT sumIf(amount, lower(status) IN ('success','completed')), countIf(lower(status) IN ('success','completed')) FROM pos.transaction_daily WHERE toYear(event_time)=$YEAR AND toMonth(event_time)=$MONTH" 2>&1 || true

echo "== DONE =="
