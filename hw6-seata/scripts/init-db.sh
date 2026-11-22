#!/usr/bin/env bash
# 重建两套逻辑分离的数据库：bookstore_inventory（库存库）、bookstore_order（订单库）
set -euo pipefail
source "$(dirname "$0")/env.sh"
mysql_q < "$HW6_ROOT/sql/01_inventory_db.sql"
mysql_q < "$HW6_ROOT/sql/02_order_db.sql"
echo "[ok] bookstore_inventory / bookstore_order 已初始化"
mysql_q -e "SELECT book_id, title, price, stock FROM bookstore_inventory.inventory"
