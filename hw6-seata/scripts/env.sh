#!/usr/bin/env bash
# 公共变量，其他脚本 source 本文件
HW6_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export HW6_ROOT
export MYSQL_USER="${MYSQL_USER:-root}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-123}"
export MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
export MYSQL_PORT="${MYSQL_PORT:-3306}"
# Seata Server（TC）解压目录，可通过环境变量覆盖
export SEATA_HOME="${SEATA_HOME:-$HW6_ROOT/seata-server-dist/apache-seata-2.1.0-incubating-bin}"
export LOG_DIR="$HW6_ROOT/logs"
export INVENTORY_URL="${INVENTORY_URL:-http://localhost:9001}"
export ORDER_URL="${ORDER_URL:-http://localhost:9002}"
mkdir -p "$LOG_DIR"

mysql_q() {
  MYSQL_PWD="$MYSQL_PASSWORD" mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" --table "$@"
}

wait_port() { # wait_port <port> <name> <timeoutSec>
  local port=$1 name=$2 t=${3:-90}
  for _ in $(seq 1 "$t"); do
    if nc -z 127.0.0.1 "$port" 2>/dev/null; then echo "[ok] $name 已监听 :$port"; return 0; fi
    sleep 1
  done
  echo "[fail] $name 在 ${t}s 内未监听 :$port" >&2; return 1
}
