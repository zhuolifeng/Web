#!/usr/bin/env bash
# hw8 一键执行：建库建表 → 灌数据 → EXPLAIN(建索引前) → 建复合索引 → EXPLAIN(建索引后) → 截图
# 用法：MYSQL_USER=root MYSQL_PASSWORD=123 ./scripts/run_all.sh
# 注意：会 DROP 并重建独立的库 bookstore_hw8，不影响业务库 bookstore。
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
EVID="$ROOT/evidence"; mkdir -p "$EVID"
export MYSQL_PWD=${MYSQL_PASSWORD:-123}
M=(mysql -u"${MYSQL_USER:-root}" -h"${MYSQL_HOST:-127.0.0.1}" -P"${MYSQL_PORT:-3306}" --default-character-set=utf8mb4)
OUT="$EVID/explain-output.txt"
: > "$OUT"

log() { echo "$*" | tee -a "$OUT"; }
q()   {  # 打印 SQL 再以表格形式执行
  log "mysql> $1"
  "${M[@]}" -t bookstore_hw8 -e "$1" 2>&1 | tee -a "$OUT"
  log ""
}
qr()  {  # 原样输出（EXPLAIN ANALYZE 的树形计划）
  log "mysql> $1"
  "${M[@]}" -N -r bookstore_hw8 -e "$1" 2>&1 | tee -a "$OUT"
  log ""
}
qv()  {  # 竖排输出（\G）
  log "mysql> $1\\G"
  "${M[@]}" -E bookstore_hw8 -e "$1" 2>&1 | tee -a "$OUT"
  log ""
}

echo "== 1. 建库建表"; "${M[@]}" < "$ROOT/sql/01_schema.sql"
echo "== 2. 生成模拟数据（10 万图书 / 1 千用户 / 1 万订单）"
start=$(date +%s); "${M[@]}" < "$ROOT/sql/02_seed.sql" >/dev/null; echo "   耗时 $(( $(date +%s) - start ))s"

COND="category = '科幻' AND price BETWEEN 50 AND 100"
log "################ hw8 EXPLAIN 分析  $(date '+%F %T')  MySQL $("${M[@]}" -N -e 'select version()')"
log ""
log "======== 0. 数据量 ========"
q "SELECT (SELECT COUNT(*) FROM books) AS books, (SELECT COUNT(*) FROM users) AS users, (SELECT COUNT(*) FROM orders) AS orders, (SELECT COUNT(*) FROM order_items) AS order_items"
q "SELECT category, COUNT(*) AS cnt, MIN(price) AS min_price, MAX(price) AS max_price FROM books GROUP BY category ORDER BY category"

log "======== 1. 建索引前 ========"
q "EXPLAIN SELECT * FROM books WHERE $COND"

log "======== 2. 创建复合索引 ========"
q "CREATE INDEX idx_category_price ON books (category, price)"
"${M[@]}" bookstore_hw8 -e "ANALYZE TABLE books" >/dev/null
q "SHOW INDEX FROM books WHERE Key_name = 'idx_category_price'"

log "======== 3. 建索引后（主查询）========"
q "EXPLAIN SELECT * FROM books WHERE $COND"
q "SELECT COUNT(*) AS matched_rows FROM books WHERE $COND"

log "======== 4. 覆盖索引 ========"
q "EXPLAIN SELECT id, category, price FROM books WHERE $COND"

log "======== 5. 排序分页（无 filesort）========"
q "EXPLAIN SELECT * FROM books WHERE $COND ORDER BY price LIMIT 20"

log "======== 6. 反例：不满足最左前缀 ========"
q "EXPLAIN SELECT * FROM books WHERE price BETWEEN 50 AND 100"

log "======== 7. EXPLAIN ANALYZE 实际耗时：走索引 vs 全表扫描 ========"
qr "EXPLAIN ANALYZE SELECT * FROM books WHERE $COND"
qr "EXPLAIN ANALYZE SELECT * FROM books IGNORE INDEX (idx_category_price) WHERE $COND"

log "======== 8. 反范式收益：我的订单列表 ========"
log "-- 范式化：JOIN users + JOIN order_items + GROUP BY"
q "EXPLAIN SELECT o.order_no, u.nickname, SUM(i.quantity) AS item_count, GROUP_CONCAT(i.book_title) AS titles, o.total_amount, o.created_at FROM orders o JOIN users u ON u.id = o.user_id JOIN order_items i ON i.order_id = o.id WHERE o.user_id = 42 GROUP BY o.id ORDER BY o.created_at DESC LIMIT 10"
log "-- 反范式：单表读取冗余字段"
q "EXPLAIN SELECT order_no, user_nickname, item_count, book_snapshot, total_amount, created_at FROM orders WHERE user_id = 42 ORDER BY created_at DESC LIMIT 10"
qv "SELECT order_no, user_nickname, item_count, book_snapshot, total_amount FROM orders WHERE user_id = 42 ORDER BY created_at DESC LIMIT 1"

echo "== 输出已保存到 $OUT"
python3 "$ROOT/scripts/render_evidence.py"
