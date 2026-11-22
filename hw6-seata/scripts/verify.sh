#!/usr/bin/env bash
# hw6 验证脚本：正常流程 / 异常回滚 / 对照组（无全局事务）
# 前置：scripts/start-all.sh 已启动三个进程
# 输出：evidence/verify-output.txt（数据库前后对比 + 接口响应 + 断言结果）
#       evidence/rollback-console.log（异常流程中三个进程与该 XID 相关的控制台日志）
set -uo pipefail
source "$(dirname "$0")/env.sh"
EVID="$HW6_ROOT/evidence"; mkdir -p "$EVID"
OUT="$EVID/verify-output.txt"
exec > >(tee "$OUT") 2>&1

PASS=0; FAIL=0
check() { # check <desc> <expected> <actual>
  if [[ "$2" == "$3" ]]; then echo "  [PASS] $1 (期望=$2, 实际=$3)"; PASS=$((PASS+1))
  else echo "  [FAIL] $1 (期望=$2, 实际=$3)"; FAIL=$((FAIL+1)); fi
}
num() { MYSQL_PWD="$MYSQL_PASSWORD" mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -N -B -e "$1"; }
stock() { num "SELECT stock FROM bookstore_inventory.inventory WHERE book_id=$1"; }
orders() { num "SELECT COUNT(*) FROM bookstore_order.orders"; }
undo() { num "SELECT (SELECT COUNT(*) FROM bookstore_inventory.undo_log)+(SELECT COUNT(*) FROM bookstore_order.undo_log)"; }
snapshot() { # snapshot <title>
  echo "---- $1 ----"
  mysql_q -e "SELECT book_id, title, stock FROM bookstore_inventory.inventory ORDER BY book_id;
              SELECT id, order_no, user_id, book_id, quantity, total_amount, status FROM bookstore_order.orders ORDER BY id;
              SELECT 'bookstore_inventory' AS db, COUNT(*) AS undo_log_rows FROM bookstore_inventory.undo_log
              UNION ALL SELECT 'bookstore_order', COUNT(*) FROM bookstore_order.undo_log;"
}
post() { # post <path> <json>
  echo "> POST $ORDER_URL$1  $2"
  curl -s -w '\n< HTTP %{http_code}\n' -H 'Content-Type: application/json' -d "$2" "$ORDER_URL$1"
}

echo "===== hw6 Seata AT 分布式事务验证  $(date '+%F %T') ====="
"$HW6_ROOT/scripts/init-db.sh" >/dev/null && echo "数据已重置：5 本书库存均为 100，订单表为空"
for f in inventory-service order-service seata-server; do : > "$LOG_DIR/$f.mark"; wc -l < "$LOG_DIR/$f.log" > "$LOG_DIR/$f.mark"; done

echo; echo "################ 用例 1：正常流程（bookId=1, quantity=2） ################"
snapshot "下单前"
post /api/orders '{"userId":1,"bookId":1,"quantity":2,"simulateError":false}'
sleep 1
snapshot "下单后"
check "库存扣减 100 → 98" 98 "$(stock 1)"
check "订单数 0 → 1" 1 "$(orders)"
sleep 2; check "全局提交后 undo_log 被异步清理" 0 "$(undo)"

echo; echo "################ 用例 2：异常流程（bookId=2, quantity=3, 订单入库时抛 RuntimeException） ################"
snapshot "下单前"
RESP=$(post /api/orders '{"userId":1,"bookId":2,"quantity":3,"simulateError":true}'); echo "$RESP"
ORDER_NO2=$(echo "$RESP" | grep -o 'orderNo=[0-9]*' | head -1 | cut -d= -f2)
sleep 2
snapshot "下单后（Seata 全局回滚之后）"
check "库存回滚：仍为 100（第一步扣减已撤销）" 100 "$(stock 2)"
check "订单未落库：订单数仍为 1" 1 "$(orders)"
check "回滚完成后 undo_log 已删除" 0 "$(undo)"

echo; echo "################ 对照组：不开启全局事务（bookId=3, quantity=5, 同样抛异常） ################"
snapshot "下单前"
post /api/orders/no-global-tx '{"userId":1,"bookId":3,"quantity":5,"simulateError":true}'
sleep 1
snapshot "下单后"
check "无全局事务时库存无法回滚：100 → 95（数据不一致）" 95 "$(stock 3)"
check "订单被本地事务回滚：订单数仍为 1" 1 "$(orders)"

# 抽取异常用例的控制台日志
LOGF="$EVID/rollback-console.log"
XID=$(grep -o 'XID=[^ ]*' "$LOG_DIR/order-service.log" | sed 's/XID=//' | grep -v null | tail -1)
# 最后一个非空 XID 即用例 2 的全局事务（对照组没有 XID）
{
  echo "# 异常流程（用例 2）全局事务 XID=$XID"
  for f in order-service inventory-service seata-server; do
    echo; echo "========== $f.log =========="
    tail -n +"$(( $(cat "$LOG_DIR/$f.mark") + 1 ))" "$LOG_DIR/$f.log" \
      | perl -pe 's/\e\[[0-9;]*m//g' \
      | grep -E "${XID//./\\.}|${ORDER_NO2:-__none__}" | head -60
  done
} > "$LOGF"
echo; echo "异常流程控制台日志已保存: $LOGF"

echo; echo "===== 结果：PASS=$PASS FAIL=$FAIL ====="
[[ $FAIL -eq 0 ]]
