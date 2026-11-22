#!/usr/bin/env bash
# 打包自己编写的源码、脚本、文档与证据（不含 target/、logs/、Seata Server 发行包等第三方/生成文件）
set -euo pipefail
source "$(dirname "$0")/env.sh"
OUT="$(dirname "$HW6_ROOT")/hw6-seata-submission.zip"
rm -f "$OUT"
cd "$(dirname "$HW6_ROOT")"
zip -qr "$OUT" "$(basename "$HW6_ROOT")" \
  -x "*/target/*" "*/logs/*" "*/seata-server-dist/*" "*/sessionStore/*" "*/.idea/*" "*.iml" "*/.DS_Store"
echo "[ok] $OUT"
unzip -l "$OUT" | tail -n +4 | awk '{print $4}' | grep -v '^$'
