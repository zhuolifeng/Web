"""Package authored source/config/docs/evidence only; never JARs or target."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--require-screenshot', action='store_true')
    args = parser.parse_args()
    screenshot = ROOT / 'evidence/auth-401-200.png'
    if args.require_screenshot and not screenshot.is_file():
        raise SystemExit('Screenshot missing. Open evidence/auth-report.html, take a real screenshot, save evidence/auth-401-200.png, then rerun.')
    fixed = ['pom.xml', '.gitignore', 'README.md', 'IMPLEMENTATION.md', 'VALIDATION.md',
             'src/main/resources/application.properties',
             'scripts/start.py', 'scripts/verify_api.py', 'scripts/benchmark.py',
             'scripts/test.sh', 'scripts/summarize_tests.py', 'scripts/package_submission.py',
             'evidence/auth-results.json', 'evidence/auth-report.html',
             'evidence/test-summary.json', 'evidence/benchmark-results.json']
    names = fixed + sorted(str(p.relative_to(ROOT)) for p in (ROOT / 'src').rglob('*.java'))
    if screenshot.is_file():
        if not screenshot.read_bytes().startswith(b'\x89PNG\r\n\x1a\n'):
            raise SystemExit('Screenshot file is not a PNG.')
        names.append('evidence/auth-401-200.png')
    files, manifest = [], []
    for name in names:
        path = ROOT / name
        if not path.is_file():
            raise SystemExit(f'Missing authored file or evidence: {name}')
        data = path.read_bytes()
        if path.suffix != '.png' and (re.search(rb'sk-[A-Za-z0-9_-]{12,}', data) or
                                     re.search(rb'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', data)):
            raise SystemExit(f'Possible credential or unredacted JWT in {name}')
        arcname = 'cloud-backend/' + name
        files.append((arcname, data))
        manifest.append({'path': arcname, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()})
    overview = ROOT.parent / 'docs/TASKS_1_TO_4_OVERVIEW.md'
    if not overview.is_file():
        raise SystemExit('Missing four-task overview document')
    overview_data = overview.read_bytes()
    overview_name = 'docs/TASKS_1_TO_4_OVERVIEW.md'
    files.append((overview_name, overview_data))
    manifest.append({'path': overview_name, 'bytes': len(overview_data),
                     'sha256': hashlib.sha256(overview_data).hexdigest()})
    if not screenshot.is_file():
        files.append(('SCREENSHOT_REQUIRED.txt',
                      '源码、实际 HTTP 报告与测试证据已包含。截图评分项尚待用户完成：按 cloud-backend/README.md 第4节截图，保存 cloud-backend/evidence/auth-401-200.png，然后使用 --require-screenshot 重新打包。\n'.encode()))
    target = ROOT.parent / 'submission/ebookstore-java21-jwt-submission.zip'
    target.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name, data in files:
            archive.writestr(name, data)
        archive.writestr('MANIFEST.json', json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    with zipfile.ZipFile(target) as archive:
        assert archive.testzip() is None
        for entry in manifest:
            assert hashlib.sha256(archive.read(entry['path'])).hexdigest() == entry['sha256']
        for name in archive.namelist():
            assert not set(Path(name).parts) & {'target', 'node_modules', 'build', '.git', '__pycache__'}
            assert not name.endswith(('.jar', '.class', '.pyc', '.env'))
    print('Created:', target)
    print(f'PASS ZIP integrity, SHA-256, exclusions and credential-pattern checks; {target.stat().st_size} bytes')
    print('Screenshot:', 'included' if screenshot.is_file() else 'NOT YET INCLUDED; see SCREENSHOT_REQUIRED.txt')


if __name__ == '__main__':
    main()
