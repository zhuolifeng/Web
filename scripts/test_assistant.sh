#!/bin/sh
# No dependency installation. Run after YOU install frontend dependencies.
set -eu
cd "$(dirname "$0")/.."
python3 -m unittest discover -s tests/assistant -v
cd frontend
CI=true npm test -- --watchAll=false --runInBand --runTestsByPath src/__tests__/AssistantPage.test.js
npm run build
