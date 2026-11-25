#!/usr/bin/env python3
"""hw7 并发压测小工具：对一个 URL 发起并发请求，统计状态码分布与每秒成功/限流数量。

用法：
  python3 scripts/hw7_loadtest.py http://localhost:8080/api/v1/book/1 -n 200 -c 20
  python3 scripts/hw7_loadtest.py URL --rps 20 --duration 5      # 匀速 20 req/s 持续 5s
仅使用 Python 标准库；结果同时写入 --out 指定的 JSON（可选）。
"""
import argparse
import json
import threading
import time
import urllib.error
import urllib.request
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor


def hit(url, t0):
    start = time.time()
    try:
        with urllib.request.urlopen(url, timeout=10) as r:
            status, body = r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        status, body = e.code, e.read().decode("utf-8", "replace")
    except Exception as e:  # 连接错误等
        status, body = -1, repr(e)
    end = time.time()
    return {"sec": int(start - t0), "status": status, "ms": round((end - start) * 1000, 1), "body": body}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("url")
    ap.add_argument("-n", type=int, default=100, help="总请求数（突发模式）")
    ap.add_argument("-c", type=int, default=20, help="并发线程数")
    ap.add_argument("--rps", type=float, default=0, help="匀速模式：每秒请求数")
    ap.add_argument("--duration", type=float, default=5, help="匀速模式：持续秒数")
    ap.add_argument("--out", help="结果 JSON 输出路径")
    a = ap.parse_args()

    t0 = time.time()
    results, lock = [], threading.Lock()

    def run(_):
        r = hit(a.url, t0)
        with lock:
            results.append(r)

    with ThreadPoolExecutor(max_workers=a.c) as pool:
        if a.rps > 0:
            total = int(a.rps * a.duration)
            for i in range(total):
                delay = t0 + i / a.rps - time.time()
                if delay > 0:
                    time.sleep(delay)
                pool.submit(run, i)
        else:
            total = a.n
            list(pool.map(run, range(total)))
    elapsed = time.time() - t0

    codes = Counter(r["status"] for r in results)
    per_sec = defaultdict(Counter)
    for r in results:
        per_sec[r["sec"]][r["status"]] += 1
    lat = sorted(r["ms"] for r in results)

    mode = f"匀速 {a.rps:g} req/s × {a.duration:g}s" if a.rps > 0 else f"突发 {a.n} 个请求"
    print(f"目标   : GET {a.url}")
    print(f"模式   : {mode}，并发线程 {a.c}")
    print(f"总请求 : {len(results)}    耗时 {elapsed:.2f}s    实际吞吐 {len(results)/elapsed:.1f} req/s")
    print(f"延迟   : p50={lat[len(lat)//2]}ms  p99={lat[int(len(lat)*0.99)-1 if len(lat)>1 else 0]}ms  max={lat[-1]}ms")
    print("状态码分布:")
    for code, cnt in sorted(codes.items()):
        name = {200: "OK", 404: "Not Found", 429: "Too Many Requests", 503: "Service Unavailable"}.get(code, "")
        print(f"  HTTP {code:<4} {name:<20} {cnt:>5}  ({cnt*100/len(results):.1f}%)")
    print("按秒统计 (第 N 秒: 成功 200 / 限流 429):")
    for sec in sorted(per_sec):
        c = per_sec[sec]
        print(f"  第 {sec} 秒: 200 × {c.get(200,0):<3}  429 × {c.get(429,0):<3}  其他 × {sum(c.values())-c.get(200,0)-c.get(429,0)}")
    for code in (429, 503):
        sample = next((r for r in results if r["status"] == code), None)
        if sample:
            print(f"HTTP {code} 响应体示例: {sample['body']}")
    ok = next((r for r in results if r["status"] == 200), None)
    if ok:
        print(f"HTTP 200 响应体示例: {ok['body'][:160]}...")

    if a.out:
        with open(a.out, "w", encoding="utf-8") as f:
            json.dump({"url": a.url, "mode": mode, "elapsed": elapsed, "codes": dict(codes),
                       "perSecond": {k: dict(v) for k, v in per_sec.items()}}, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
