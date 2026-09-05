#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Start all Java services LOCALLY (java -jar) against the infra containers
# ─────────────────────────────────────────────────────────────────────────────
set -uo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
LOGDIR="$ROOT/e2e/logs"
mkdir -p "$LOGDIR"

# ── infra endpoints ─────────────────────────────────────────────────────────
export DB_HOST="${DB_HOST:-localhost}"
export DB_PORT="${DB_PORT:-5432}"
export DB_USERNAME="${DB_USERNAME:-DRAGON}"
export DB_PASSWORD="${DB_PASSWORD:-DRAGON}"
export DB_USER="${DB_USER:-DRAGON}"
export DB_PASS="${DB_PASS:-DRAGON}"
export DB_NAME="${DB_NAME:-point_of_sale}"
export REDIS_HOSTS="${REDIS_HOSTS:-redis://:dragon_knight@localhost:6381}"
export QUARKUS_REDIS_HOSTS="$REDIS_HOSTS"
export QUARKUS_REDIS_CLIENT_TYPE="standalone"
export KAFKA_BROKERS="${KAFKA_BROKERS:-localhost:9092}"
export OTEL_ENDPOINT="${OTEL_ENDPOINT:-localhost:4317}"
export APP_ENV="local"

# gRPC client host/port mapping
export AUTH_HOST=localhost AUTH_GRPC_PORT=9012
export USER_HOST=localhost USER_GRPC_PORT=9011
export USER_SERVICE_HOST=localhost USER_SERVICE_GRPC_PORT=9011
export ROLE_HOST=localhost ROLE_GRPC_PORT=9006
export MERCHANT_HOST=localhost MERCHANT_GRPC_PORT=9005
export TRANSACTION_HOST=localhost TRANSACTION_GRPC_PORT=9009
export CASHIER_HOST=localhost CASHIER_GRPC_PORT=9014
export CATEGORY_HOST=localhost CATEGORY_GRPC_PORT=9015
export PRODUCT_HOST=localhost PRODUCT_GRPC_PORT=9003
export ORDER_HOST=localhost ORDER_GRPC_PORT=9001
export ORDER_ITEM_HOST=localhost ORDER_ITEM_GRPC_PORT=9016
export STATS_READER_HOST=localhost STATS_READER_GRPC_PORT=9029

# ── commands ────────────────────────────────────────────────────────────────
stop() {
    echo "==> Stopping local JVMs..."
    pkill -f 'quarkus-run.jar' 2>/dev/null || true
    pkill -f 'target/quarkus-app' 2>/dev/null || true
    echo "==> Stopped."
    exit 0
}
[ "${1:-}" = "stop" ] && stop
[ "${1:-}" = "--package" ] && { echo "==> Packaging..."; mvn clean package -DskipTests -q; }

# ── JVM helper ──────────────────────────────────────────────────────────────
start() {
    local name="$1"; shift
    local http_port="$1"; shift
    local jar="$ROOT/$name/target/quarkus-app/quarkus-run.jar"
    if [ ! -f "$jar" ]; then
        echo "!! $name: jar not found, run: mvn clean package -DskipTests"; return 0
    fi
    echo "==> Starting $name (http:$http_port)"
    setsid nohup java -Xmx512m \
        -Dquarkus.http.port="$http_port" \
        "$@" \
        -jar "$jar" \
        > "$LOGDIR/$name.log" 2>&1 < /dev/null &
    disown 2>/dev/null || true
    echo "$!" >> "$LOGDIR/pids"
}

# wait_for_health <port> <max_seconds>
wait_for_health() {
    local port="$1"
    local max_wait="${2:-90}"
    local elapsed=0
    while [ $elapsed -lt $max_wait ]; do
        if curl -sf -o /dev/null "http://localhost:$port/q/health" 2>/dev/null; then
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done
    echo "!! WARNING: port $port not healthy after ${max_wait}s"
    return 0
}

rm -f "$LOGDIR/pids"

# ── Wave 1: User service first (creates all schemas + user tables) ──────────
echo "==> Wave 1: user (creates schemas)"
start user 8091
wait_for_health 8091 120
echo "==> User healthy, schemas created"

# ── Wave 2: Role service (depends on user) ─────────────────────────────────
echo "==> Wave 2: role"
start role 8086
wait_for_health 8086 90

# ── Wave 3a: Merchant (creates pos_merchant tables) ─────────────────────────
echo "==> Wave 3a: merchant"
start merchant 8085
wait_for_health 8085 120

echo "==> Wave 3b: category, auth (depend on merchant tables)"
start category 8087
start auth 8092
wait_for_health 8087 90
wait_for_health 8092 90

# ── Wave 4: Product, cashier (depend on merchant tables) ────────────────────
echo "==> Wave 4: product, cashier"
start product 8088
start cashier 8089
wait_for_health 8088 90
wait_for_health 8089 90

# ── Wave 5: Order, order_item (depend on merchant + product) ────────────────
echo "==> Wave 5: order, order_item"
start order 8094
start order_item 8093
wait_for_health 8094 120
wait_for_health 8093 90

# ── Wave 6: Transaction (depends on orders) ─────────────────────────────────
echo "==> Wave 6: transaction"
start transaction 8097
wait_for_health 8097 90

# ── Wave 7: Email, stats, gateway ───────────────────────────────────────────
echo "==> Wave 7: email, stats, gateway"
start email-service 8098
start stats-reader 8096
start stats-writer 8100
wait_for_health 8098 60
start gateway 5000
wait_for_health 5000 90

echo ""
echo "==> All services launched. Logs: $LOGDIR/"
echo "==> Gateway: http://localhost:5000"
echo "==> Stop:    $0 stop"
