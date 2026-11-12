"""Package only assignment sources/docs/evidence; never dependencies or secrets."""
import hashlib
import json
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
FILES = [
    'assistant/__init__.py', 'assistant/server.py', 'assistant/config.example.sh',
    'assistant/README.md', 'assistant/VALIDATION.md', 'assistant/TASK2_IMPLEMENTATION.md',
    'assistant/live-normal.json', 'assistant/live-invalid.json', 'assistant/live-timeout.json',
    'frontend/src/pages/AssistantPage.js', 'frontend/src/pages/AssistantPage.css',
    'frontend/src/App.js', 'frontend/src/components/Layout.js',
    'frontend/src/__tests__/AssistantPage.test.js', 'tests/assistant/test_assistant.py',
    'scripts/run_assistant_demo.sh', 'scripts/run_assistant_live.py',
    'scripts/test_assistant.sh', 'scripts/smoke_assistant.py',
    'scripts/package_assistant_submission.py',
]


def main():
    entries, manifest = [], []
    for name in FILES:
        path = ROOT / name
        if not path.is_file():
            raise SystemExit(f'Missing expected submission file: {name}')
        content = path.read_bytes()
        if re.search(rb'sk-[A-Za-z0-9_-]{12,}', content):
            raise SystemExit(f'Possible API key found; refusing to package: {name}')
        entries.append((name, content))
        manifest.append({'path': name, 'bytes': len(content), 'sha256': hashlib.sha256(content).hexdigest()})
    output = ROOT / 'submission' / 'ebookstore-assistant-submission.zip'
    output.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name, content in entries:
            archive.writestr(name, content)
        archive.writestr('MANIFEST.json', json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    with zipfile.ZipFile(output) as archive:
        assert archive.testzip() is None
        assert len(archive.namelist()) == len(FILES) + 1
        for name in archive.namelist():
            assert not set(Path(name).parts) & {'node_modules', 'target', 'build', '.git', '__pycache__'}
            assert not name.endswith(('.jar', '.class', '.pyc', '.env'))
    print(f'Created {output.relative_to(ROOT)} ({output.stat().st_size} bytes, {len(FILES) + 1} files)')
    print('ZIP integrity, allowlist and API-key pattern checks: PASS')


if __name__ == '__main__':
    main()
