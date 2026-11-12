#!/bin/sh
# Uses installed dependencies only; performs no installation.
set -eu
cd "$(dirname "$0")/.."
python3 -m unittest discover -s tests/guide -v
cd frontend
CI=true npm test -- --watchAll=false --runInBand --runTestsByPath src/__tests__/GuidePage.test.js src/__tests__/AssistantPage.test.js
npm run build
