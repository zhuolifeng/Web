"""Build an allowlisted Task 3 submission, excluding third-party artifacts and keys."""
import hashlib
import json
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
FILES = [
    'guide/__init__.py', 'guide/rag.py', 'guide/agent.py', 'guide/server.py',
    'guide/data/store_policy.md', 'guide/data/catalog.json', 'guide/data/policy_index.json',
    'guide/README.md', 'guide/TASK3_IMPLEMENTATION.md', 'guide/VALIDATION.md',
    'guide/evidence/demo-case.json', 'guide/evidence/demo-case.log',
    'guide/evidence/live-case.json', 'guide/evidence/live-case.log',
    'assistant/__init__.py', 'assistant/server.py',
    'frontend/src/App.js', 'frontend/src/components/Layout.js',
    'frontend/src/pages/GuidePage.js', 'frontend/src/pages/GuidePage.css',
    'frontend/src/pages/AssistantPage.js', 'frontend/src/pages/AssistantPage.css',
    'frontend/src/__tests__/GuidePage.test.js', 'frontend/src/__tests__/AssistantPage.test.js',
    'tests/guide/test_guide.py', 'scripts/run_guide.py', 'scripts/run_guide_case.py',
    'scripts/build_policy_index.py', 'scripts/verify_guide_artifacts.py',
    'scripts/test_guide.sh', 'scripts/package_guide_submission.py',
]


def main():
    content, manifest = [], []
    for name in FILES:
        path = ROOT / name
        if not path.is_file():
            raise SystemExit(f'Missing submission file: {name}')
        data = path.read_bytes()
        if re.search(rb'sk-[A-Za-z0-9_-]{12,}', data):
            raise SystemExit(f'Possible API key found, refusing to package {name}')
        content.append((name, data))
        manifest.append({'path': name, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()})
    target = ROOT / 'submission/ebookstore-task3-submission.zip'
    target.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name, data in content:
            archive.writestr(name, data)
        archive.writestr('MANIFEST.json', json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    with zipfile.ZipFile(target) as archive:
        assert archive.testzip() is None
        assert set(archive.namelist()) == set(FILES) | {'MANIFEST.json'}
        for item in json.loads(archive.read('MANIFEST.json')):
            assert hashlib.sha256(archive.read(item['path'])).hexdigest() == item['sha256']
        for name in archive.namelist():
            assert not set(Path(name).parts) & {'node_modules', 'target', 'build', '.git', '__pycache__'}
            assert not name.endswith(('.jar', '.class', '.pyc', '.env'))
    print('Created:', target)
    print(f'PASS ZIP integrity, allowlist, SHA-256 and key-pattern check: {len(FILES)+1} files, {target.stat().st_size} bytes')


if __name__ == '__main__':
    main()
