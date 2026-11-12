#!/bin/sh
# No downloads; uses the current Java 21+ environment (Java 24 is accepted).
set -eu
cd "$(dirname "$0")/.."
if [ -n "${JAVA_HOME:-}" ]; then
  PATH="$JAVA_HOME/bin:$PATH"
  export PATH
fi
python3 - <<'PY'
import re
import subprocess
result = subprocess.run(['java', '-version'], capture_output=True, text=True)
version = re.search(r'version "(\d+)', result.stderr + result.stdout)
if not version or int(version.group(1)) < 21:
    raise SystemExit('Java 21+ required; Java 24 is supported')
print('Using Java', version.group(1), '(minimum 21)')
PY
mvn -o test
python3 scripts/summarize_tests.py
