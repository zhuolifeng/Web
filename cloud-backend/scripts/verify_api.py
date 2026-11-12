"""Real HTTP checks and a screenshot-ready report; never save JWTs or passwords."""
import argparse
from datetime import datetime, timezone
import html
import json
import os
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]


def request(base, path, payload=None, token=None, cookie=None):
    headers = {'Content-Type': 'application/json'}
    if token is not None:
        headers['Authorization'] = 'Bearer ' + token
    if cookie:
        headers['Cookie'] = cookie
    req = Request(base.rstrip('/') + path, headers=headers,
                  data=None if payload is None else json.dumps(payload).encode())
    try:
        with urlopen(req, timeout=15) as response:
            return response.status, dict(response.headers.items()), json.load(response)
    except HTTPError as exc:
        try:
            return exc.code, dict(exc.headers.items()), json.load(exc)
        finally:
            exc.close()
    except URLError as exc:
        raise SystemExit('无法连接服务，请先运行 python3 cloud-backend/scripts/start.py') from exc


def login(base):
    status, _, body = request(base, '/api/auth/token', {
        'username': os.environ.get('DEMO_USERNAME', 'student'),
        'password': os.environ.get('DEMO_PASSWORD', 'demo-password')})
    if status != 200:
        raise SystemExit('登录未成功，请确认 DEMO_USERNAME / DEMO_PASSWORD 与服务一致。')
    token = body.get('data', {}).get('accessToken')
    if not isinstance(token, str) or token.count('.') != 2:
        raise SystemExit('服务未返回 JWT。')
    return token


def render_report(report, target):
    cards = []
    for case in report['cases']:
        good = case['passed']
        detail = json.dumps({'responseHeaders': case['responseHeaders'], 'body': case['body']}, ensure_ascii=False, indent=2)
        cards.append(f'''<article><div class="row"><h2>{html.escape(case['name'])}</h2>
<span class="status">HTTP {case['status']}</span></div><p>{html.escape(case['request'])}</p>
<p class="{'pass' if good else 'fail'}">{'PASS' if good else 'FAIL'} · expected {case['expectedStatus']}</p>
<pre>{html.escape(detail)}</pre></article>''')
    page = '''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>JWT 与虚拟线程实际 HTTP 验收</title><style>
body{font-family:system-ui,sans-serif;background:#f3f6fa;color:#172b4d;max-width:1200px;margin:30px auto;padding:0 20px}
h1{font-size:28px}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}article{background:white;border:1px solid #dbe3ef;border-radius:14px;padding:20px}
h2{font-size:18px;margin:0}.row{display:flex;justify-content:space-between;gap:10px;align-items:center}.status{background:#edf2ff;padding:6px 10px;border-radius:7px;white-space:nowrap;font-weight:700}
.pass{color:#067647;font-weight:bold}.fail{color:#b42318}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#f8fafc;padding:12px;font-size:12px;line-height:1.5}
small{color:#52616b} @media(max-width:800px){.grid{grid-template-columns:1fr}}
</style><h1>JWT 无状态认证 + Web 虚拟线程验收</h1>'''
    page += f"<p>实际请求地址：{html.escape(report['baseUrl'])} · {html.escape(report['timestamp'])}</p>"
    page += '<p>本页由实际 HTTP 请求结果生成，已隐藏 Token。前两张卡片用于截取 401 与 200 验收图。</p>'
    page += '<div class="grid">' + ''.join(cards) + '</div><p><small>不是伪造浏览器 Network 截图；请在浏览器打开本报告后实际截图提交。虚拟线程证据来自服务端 Thread.currentThread().isVirtual()。</small></p></html>'
    target.write_text(page, encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', default='http://127.0.0.1:8081')
    parser.add_argument('--allow-other-java', action='store_true', help='Compatibility flag; Java 21+ is accepted by default')
    parser.add_argument('--output-dir', default=str(ROOT / 'evidence'))
    args = parser.parse_args()
    token = login(args.url)
    checks = [
        ('未携带 Token：401 拦截', '/api/books/1', None, None, 401),
        ('有效 JWT：200 成功 + 虚拟线程', '/api/books/1', token, None, 200),
        ('错误 Token：401 拦截', '/api/books/1', 'invalid-token', None, 401),
        ('仅有 Session Cookie：401', '/api/books/1', None, 'JSESSIONID=not-authenticated', 401),
        ('成功请求后不带 Token：仍为 401', '/api/books/1', None, None, 401),
        ('非法 ID：400', '/api/books/0', token, None, 400),
        ('图书不存在：404', '/api/books/99999', token, None, 404),
    ]
    report = {'timestamp': datetime.now(timezone.utc).isoformat(), 'baseUrl': args.url,
              'minimumJavaVersion': 21, 'cases': []}
    all_passed = True
    for name, path, jwt, cookie, expected in checks:
        status, headers, body = request(args.url, path, token=jwt, cookie=cookie)
        normalized = {k.lower(): v for k, v in headers.items()}
        passed = status == expected and body.get('code') == expected and 'set-cookie' not in normalized
        if status == 200:
            passed = passed and normalized.get('x-request-thread-virtual') == 'true'
            major = normalized.get('x-java-version', '').split('.')[0]
            passed = passed and major.isdigit() and int(major) >= 21
        all_passed = all_passed and passed
        safe_headers = {k: v for k, v in headers.items() if k.lower() in (
            'content-type', 'www-authenticate', 'x-request-thread-virtual', 'x-request-thread-name', 'x-java-version')}
        report['cases'].append({'name': name, 'request': 'GET ' + path + (' · Authorization: Bearer [REDACTED]' if jwt else ''),
                                'status': status, 'expectedStatus': expected, 'passed': passed,
                                'responseHeaders': safe_headers, 'body': body})
        print(('PASS' if passed else 'FAIL'), name, status,
              'virtual=' + normalized.get('x-request-thread-virtual', '?'),
              'java=' + normalized.get('x-java-version', '?'))
    target = Path(args.output_dir)
    target.mkdir(parents=True, exist_ok=True)
    (target / 'auth-results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    render_report(report, target / 'auth-report.html')
    print('Saved:', target / 'auth-report.html')
    if not all_passed:
        raise SystemExit('Some checks failed. The report preserves actual results; inspect Java version and virtual-thread headers.')


if __name__ == '__main__':
    main()
