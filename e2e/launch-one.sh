#!/usr/bin/env bash
# Launch one service detached with the full local env.
# Usage: launch-one.sh <module> <http-port>
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MOD="$1"; HTTP_PORT="$2"

export DB_HOST="${DB_HOST:-localhost}" DB_PORT="${DB_PORT:-5432}"
export DB_USERNAME="${DB_USERNAME:-DRAGON}" DB_PASSWORD="${DB_PASSWORD:-DRAGON}"
export DB_USER="${DB_USER:-DRAGON}" DB_PASS="${DB_PASS:-DRAGON}"
export DB_NAME="${DB_NAME:-POINT_OF_SALE}"
export REDIS_HOSTS="${REDIS_HOSTS:-redis://:dragon_knight@localhost:6379}"
export QUARKUS_REDIS_HOSTS="$REDIS_HOSTS"
export QUARKUS_REDIS_CLIENT_TYPE="${QUARKUS_REDIS_CLIENT_TYPE:-cluster}"
export KAFKA_BROKERS="${KAFKA_BROKERS:-localhost:9092}"
export OTEL_ENDPOINT="${OTEL_ENDPOINT:-localhost:4317}"
export APP_ENV="local"
export AUTH_HOST=localhost AUTH_GRPC_PORT=9012
export USER_HOST=localhost USER_GRPC_HOST=localhost USER_GRPC_PORT=9011
export ROLE_HOST=localhost ROLE_GRPC_HOST=localhost ROLE_GRPC_PORT=9006
export MERCHANT_HOST=localhost MERCHANT_GRPC_PORT=9005
export TRANSACTION_HOST=localhost TRANSACTION_GRPC_PORT=9009
export CASHIER_HOST=localhost CASHIER_GRPC_PORT=9014
export CATEGORY_HOST=localhost CATEGORY_GRPC_PORT=9015
export PRODUCT_HOST=localhost PRODUCT_GRPC_PORT=9003
export ORDER_HOST=localhost ORDER_GRPC_PORT=9001
export ORDER_ITEM_HOST=localhost ORDER_ITEM_GRPC_PORT=9016

exec java -Xmx512m -Dquarkus.http.port="$HTTP_PORT" \
    -jar "$ROOT/$MOD/target/quarkus-app/quarkus-run.jar"
