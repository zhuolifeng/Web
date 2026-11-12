"""Small real HTTP I/O benchmark; does not claim production-scale performance."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import json
import math
from pathlib import Path
import time
from verify_api import ROOT, login, request


def measure(base, count, concurrency):
    token = login(base)
    for _ in range(5):
        request(base, '/api/books/1', token=token)
    def one(_):
        started = time.perf_counter()
        status, headers, _ = request(base, '/api/books/1', token=token)
        values = {k.lower(): v for k, v in headers.items()}
        return (time.perf_counter() - started) * 1000, status, values.get('x-request-thread-virtual'), values.get('x-java-version')
    started = time.perf_counter()
    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        results = list(executor.map(one, range(count)))
    elapsed = time.perf_counter() - started
    latencies = sorted(r[0] for r in results)
    return {'url': base, 'requests': count, 'concurrency': concurrency, 'elapsedSeconds': round(elapsed, 4),
            'requestsPerSecond': round(count / elapsed, 2), 'meanMs': round(sum(latencies)/count, 2),
            'p95Ms': round(latencies[math.ceil(count * .95) - 1], 2),
            'successes': sum(r[1] == 200 for r in results),
            'virtualHeaderValues': sorted({r[2] or 'missing' for r in results}),
            'javaVersions': sorted({r[3] or 'missing' for r in results})}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', default='http://127.0.0.1:8081')
    parser.add_argument('--platform-url', help='Optional separately running --platform control instance')
    parser.add_argument('--requests', type=int, default=100)
    parser.add_argument('--concurrency', type=int, default=50)
    args = parser.parse_args()
    if not 1 <= args.requests <= 2000 or not 1 <= args.concurrency <= 200:
        raise SystemExit('Use 1–2000 requests and 1–200 concurrent clients.')
    report = {'timestamp': datetime.now(timezone.utc).isoformat(), 'kind': 'local synthetic blocking I/O HTTP benchmark',
              'virtual': measure(args.url, args.requests, args.concurrency)}
    if args.platform_url:
        report['platform'] = measure(args.platform_url, args.requests, args.concurrency)
        report['measuredThroughputRatio'] = round(report['virtual']['requestsPerSecond'] / report['platform']['requestsPerSecond'], 3)
    if report['virtual']['virtualHeaderValues'] != ['true']:
        raise SystemExit('The supposed virtual-thread service did not report virtual=true.')
    if args.platform_url and report['platform']['virtualHeaderValues'] != ['false']:
        raise SystemExit('The platform control service did not report virtual=false.')
    path = ROOT / 'evidence/benchmark-results.json'
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(report, ensure_ascii=False, indent=2))
    print('Saved:', path)
    if any(r.get('successes') != args.requests for k,r in report.items() if k in ('virtual', 'platform')):
        raise SystemExit('Benchmark contains failed requests; do not treat it as a successful performance result.')
