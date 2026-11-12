#!/bin/sh
# No dependency installation. Stop with Ctrl+C.
set -eu
cd "$(dirname "$0")/.."
export ASSISTANT_MODE=demo
export INVENTORY_MODE=demo
exec python3 assistant/server.py
