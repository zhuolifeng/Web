#!/usr/bin/env bash
# 停止 start-all.sh 启动的进程（仅按 logs/*.pid 停止，不影响其他 Java 进程）
source "$(dirname "$0")/env.sh"
for name in order-service inventory-service seata-server; do
  pidf="$LOG_DIR/$name.pid"
  if [[ -f "$pidf" ]] && kill -0 "$(cat "$pidf")" 2>/dev/null; then
    kill "$(cat "$pidf")" && echo "[stop] $name pid=$(cat "$pidf")"
  fi
  rm -f "$pidf"
done
