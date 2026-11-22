#!/usr/bin/env bash
# 启动 Seata Server(TC, :8091) → inventory-service(:9001) → order-service(:9002)
# 进程以 nohup 方式后台运行，日志在 logs/ 下，PID 在 logs/*.pid
set -euo pipefail
source "$(dirname "$0")/env.sh"

start() { # start <name> <cmd...>
  local name=$1; shift
  local pidf="$LOG_DIR/$name.pid"
  if [[ -f "$pidf" ]] && kill -0 "$(cat "$pidf")" 2>/dev/null; then
    echo "[skip] $name 已在运行 (pid $(cat "$pidf"))"; return
  fi
  nohup "$@" > "$LOG_DIR/$name.log" 2>&1 &
  echo $! > "$pidf"
  echo "[start] $name pid=$! log=$LOG_DIR/$name.log"
}

# 1. Seata Server（file 存储模式，无需注册中心）
if [[ ! -d "$SEATA_HOME" ]]; then
  echo "未找到 Seata Server: $SEATA_HOME" >&2
  echo "请下载 https://archive.apache.org/dist/incubator/seata/2.1.0/apache-seata-2.1.0-incubating-bin.tar.gz 并解压到 seata-server-dist/，或设置 SEATA_HOME" >&2
  exit 1
fi
start seata-server java \
  --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  -Dspring.config.additional-location="$SEATA_HOME/conf/" \
  -Dspring.config.location="$SEATA_HOME/conf/application.yml" \
  -Dlogging.config="$SEATA_HOME/conf/logback-spring.xml" \
  -Dlog.home="$LOG_DIR/seata-server-detail" -Dloader.path="$SEATA_HOME/lib" -Xmx512m \
  -jar "$SEATA_HOME/target/seata-server.jar" -p 8091 -m file
wait_port 8091 seata-server 120

# 2. 业务服务
if [[ ! -f "$HW6_ROOT/order-service/target/order-service-0.0.1-SNAPSHOT.jar" ]]; then
  (cd "$HW6_ROOT" && mvn -q -DskipTests package)
fi
start inventory-service java -jar "$HW6_ROOT/inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar"
start order-service java -jar "$HW6_ROOT/order-service/target/order-service-0.0.1-SNAPSHOT.jar"
wait_port 9001 inventory-service 120
wait_port 9002 order-service 120
echo "全部启动完成。查看日志: tail -f $LOG_DIR/{seata-server,inventory-service,order-service}.log"
