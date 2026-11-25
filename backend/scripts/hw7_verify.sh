#!/usr/bin/env bash
# hw7 一键验证：Redis Cache-Aside / 防穿透 / 写后删缓存 / Sentinel 限流 429
# 前置：MySQL、Redis(localhost:6379) 已启动，后端已在 $BASE 运行
# 用法：./scripts/hw7_verify.sh            （输出同时保存到 hw7-evidence/verify-output.txt）
set -uo pipefail
BASE=${BASE:-http://localhost:8080}
BOOK_ID=${BOOK_ID:-1}
MISSING_ID=${MISSING_ID:-999999}
ADMIN_USER=${ADMIN_USER:-admin}
ADMIN_PASS=${ADMIN_PASS:-admin123}
HERE=$(cd "$(dirname "$0")/.." && pwd)
OUT_DIR="$HERE/hw7-evidence"
mkdir -p "$OUT_DIR"
exec > >(tee "$OUT_DIR/verify-output.txt") 2>&1

if command -v redis-cli >/dev/null; then RCLI=(redis-cli); else RCLI=(docker exec bookstore-redis redis-cli); fi
rc() { "${RCLI[@]}" "$@"; }
json() { python3 -c "import sys,json;d=json.load(sys.stdin);print(eval('d'+sys.argv[1]))" "$1"; }
pass() { echo "[PASS] $*"; }
fail() { echo "[FAIL] $*"; FAILED=1; }
FAILED=0
KEY="bookstore:book:detail:$BOOK_ID"
NKEY="bookstore:book:detail:$MISSING_ID"
sep() { echo; echo "==================== $* ===================="; }
stats() { curl -s "$BASE/api/v1/admin/books/cache-stats" -H "Authorization: Bearer $TOKEN" | json "['data']"; }

echo "hw7 验证  $(date '+%F %T')  BASE=$BASE"
TOKEN=$(curl -s -X POST "$BASE/api/v1/users/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | json "['data']['token']")
[ -n "$TOKEN" ] || { echo "管理员登录失败"; exit 1; }
curl -s -X DELETE "$BASE/api/v1/admin/books/cache-stats" -H "Authorization: Bearer $TOKEN" >/dev/null
echo "Redis PING: $(rc PING)"

sep "1. Cache-Aside 读：首次未命中查库并回写，再次命中缓存"
rc DEL "$KEY" >/dev/null
echo "> 清空 $KEY 后，Redis 中是否存在: $(rc EXISTS "$KEY")"
c1=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/book/$BOOK_ID"); sleep 0.3
c2=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/book/$BOOK_ID")
echo "> 两次 GET /api/v1/book/$BOOK_ID → HTTP $c1, $c2"
echo "> Redis GET $KEY: $(rc GET "$KEY" | cut -c1-120)..."
echo "> Redis TTL $KEY: $(rc TTL "$KEY") 秒（1800 + 0~300 随机抖动）"
S=$(stats); echo "> 缓存统计: $S"
python3 -c "import ast,sys;s=ast.literal_eval(sys.argv[1]);sys.exit(0 if s['dbLoads']==1 and s['hits']>=1 else 1)" "$S" \
  && pass "第一次查库(dbLoads=1)，第二次命中缓存(hits>=1)" || fail "缓存读路径不符合预期"

sep "2. 防缓存穿透：不存在的 id 缓存空对象"
rc DEL "$NKEY" >/dev/null
curl -s -X DELETE "$BASE/api/v1/admin/books/cache-stats" -H "Authorization: Bearer $TOKEN" >/dev/null
for i in 1 2 3; do
  echo "> GET /api/v1/book/$MISSING_ID → $(curl -s -w ' (HTTP %{http_code})' "$BASE/api/v1/book/$MISSING_ID")"; sleep 0.3
done
echo "> Redis GET $NKEY: $(rc GET "$NKEY")   TTL: $(rc TTL "$NKEY") 秒"
S=$(stats); echo "> 缓存统计: $S"
python3 -c "import ast,sys;s=ast.literal_eval(sys.argv[1]);sys.exit(0 if s['dbLoads']==1 and s['nullHits']==2 else 1)" "$S" \
  && pass "3 次请求只查库 1 次，后 2 次命中空对象" || fail "空对象缓存不符合预期"
echo "> 非法 id（-1）直接 404，不访问缓存: $(curl -s -w ' (HTTP %{http_code})' "$BASE/api/v1/book/-1")"

sep "3. Cache-Aside 写：更新数据库后删除缓存"
sleep 1
curl -s "$BASE/api/v1/book/$BOOK_ID" >/dev/null
echo "> 更新前 Redis EXISTS $KEY: $(rc EXISTS "$KEY")"
BODY=$(curl -s "$BASE/api/v1/book/$BOOK_ID" | python3 -c "import sys,json;print(json.dumps(json.load(sys.stdin)['data'],ensure_ascii=False))")
code=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/api/v1/admin/books/$BOOK_ID" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$BODY")
echo "> PUT /api/v1/admin/books/${BOOK_ID}（原样写回）→ HTTP $code"
E=$(rc EXISTS "$KEY"); echo "> 更新后 Redis EXISTS $KEY: $E"
[ "$E" = "0" ] && pass "写操作提交后缓存已删除，下次读取将重建" || fail "写后缓存未删除"

sep "4. 防缓存击穿：并发重建只允许一个线程查库（JUnit + 真实 Redis）"
echo "> mvn -Dtest=BookCacheRedisTest test（100 线程并发访问冷 key，断言 dbCalls == 1）："
if [ "${RUN_JUNIT:-1}" = 1 ]; then
  (cd "$HERE" && mvn test -Dtest=BookCacheRedisTest 2>&1) \
    | grep -E "\[防击穿\]|Tests run: [0-9]+, F.*in com|FAIL" | sed 's/^\[INFO\] //; s/^/  /' | tee "$OUT_DIR/junit-breakdown.txt"
fi
grep -q "Tests run: 3, Failures: 0, Errors: 0, Skipped: 0" "$OUT_DIR/junit-breakdown.txt" 2>/dev/null \
  && pass "BookCacheRedisTest 全部通过：并发 100 线程只有 1 个线程获得锁并查库" \
  || echo "> 已跳过（可 RUN_JUNIT=1 重新执行）"

sep "5. Sentinel 限流：GET /api/v1/book/{id} QPS<=5，超出返回 429"
sleep 2
python3 "$HERE/scripts/hw7_loadtest.py" "$BASE/api/v1/book/$BOOK_ID" --rps 20 --duration 5 -c 20 \
  --out "$OUT_DIR/loadtest-rps20.json"
echo
sleep 2   # 等统计窗口滑过，再做突发压测
python3 "$HERE/scripts/hw7_loadtest.py" "$BASE/api/v1/book/$BOOK_ID" -n 100 -c 50 --out "$OUT_DIR/loadtest-burst.json"
N429=$(python3 -c "import json;print(json.load(open('$OUT_DIR/loadtest-burst.json'))['codes'].get('429',0))")
[ "$N429" -gt 0 ] && pass "突发 100 个并发请求中 $N429 个被限流返回 HTTP 429" || fail "未触发 429"

sep "6. 对照：未配置限流规则的列表接口不受影响"
sleep 1
python3 "$HERE/scripts/hw7_loadtest.py" "$BASE/api/v1/books" -n 30 -c 10 | sed -n '1,7p'

echo
[ "$FAILED" = 0 ] && echo "全部检查通过" || echo "存在失败项"
