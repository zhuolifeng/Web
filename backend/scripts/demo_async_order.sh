#!/usr/bin/env bash
# hw5 命令行演示：登录 → 加购 → POST /api/orders (202) → 订阅 SSE 直到收到最终结果
# 用法：DEMO_USER=xxx DEMO_PASS=yyy BOOK_ID=1 QTY=1 ./scripts/demo_async_order.sh
set -euo pipefail
BASE=${BASE:-http://localhost:8080}
DEMO_USER=${DEMO_USER:?请设置 DEMO_USER}
DEMO_PASS=${DEMO_PASS:?请设置 DEMO_PASS}
BOOK_ID=${BOOK_ID:-1}
QTY=${QTY:-1}

json() { python3 -c "import sys,json;d=json.load(sys.stdin);print(eval('d'+sys.argv[1]))" "$1"; }

echo "== 1. 登录"
TOKEN=$(curl -s -X POST "$BASE/api/v1/users/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$DEMO_USER\",\"password\":\"$DEMO_PASS\"}" | json "['data']['token']")
echo "token=${TOKEN:0:20}..."

echo "== 2. 加入购物车 bookId=$BOOK_ID qty=$QTY"
curl -s -X POST "$BASE/api/v1/cart" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"bookId\":$BOOK_ID,\"quantity\":$QTY}"; echo

echo "== 3. 提交订单（应立即返回 202 Accepted）"
START=$(python3 -c 'import time;print(time.time())')
RESP=$(curl -s -i -X POST "$BASE/api/orders" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"receiver":"张三","phone":"13800138000","province":"上海","city":"上海","address":"东川路800号","payment":"wechat"}')
END=$(python3 -c 'import time;print(time.time())')
echo "$RESP"
python3 -c "print('POST 耗时 %.0f ms' % (($END-$START)*1000))"
QUEUE_ID=$(echo "$RESP" | tail -1 | json "['data']['queueId']")

echo "== 4. 订阅 SSE /api/orders/$QUEUE_ID/stream（处理完成后服务端自动关闭连接）"
curl -s -N "$BASE/api/orders/$QUEUE_ID/stream" -H "Authorization: Bearer $TOKEN" -H 'Accept: text/event-stream' \
  | while IFS= read -r line; do printf '[%s] %s\n' "$(date +%H:%M:%S)" "$line"; done
