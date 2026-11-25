#!/usr/bin/env python3
"""把 hw7_verify.sh 的真实输出渲染成 HTML，并用本机 Chrome headless 截图到 hw7-evidence/*.png。

用法：python3 scripts/hw7_render_evidence.py
只读取 hw7-evidence/verify-output.txt，不访问后端、不修改数据。
"""
import html
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EVID = ROOT / "hw7-evidence"
CHROME = next((c for c in ["/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                           shutil.which("google-chrome") or "", shutil.which("chromium") or ""]
               if c and Path(c).exists()), None)

CSS = """
body{margin:0;background:#0f1115;color:#d7dae0;font:13px/1.5 Menlo,Consolas,monospace}
h1{font:600 18px -apple-system,sans-serif;margin:0 0 4px;color:#fff}
.sub{color:#8b93a1;font:12px -apple-system,sans-serif;margin-bottom:14px}
.wrap{padding:18px 22px}
pre{white-space:pre-wrap;word-break:break-all;margin:0;background:#161a21;border:1px solid #262c36;border-radius:6px;padding:12px}
.pass{color:#4ade80;font-weight:700}.fail{color:#f87171;font-weight:700}
.sec{color:#c084fc;font-weight:700}.r429{color:#fbbf24;font-weight:700}.ok{color:#4ade80}
"""


def colorize(text: str) -> str:
    out = []
    for line in html.escape(text).splitlines():
        if "[PASS]" in line:
            line = f'<span class="pass">{line}</span>'
        elif "[FAIL]" in line:
            line = f'<span class="fail">{line}</span>'
        elif line.startswith("===="):
            line = f'<span class="sec">{line}</span>'
        elif "429" in line:
            line = f'<span class="r429">{line}</span>'
        elif re.search(r"HTTP 200|命中", line):
            line = f'<span class="ok">{line}</span>'
        out.append(line)
    return "\n".join(out)


def page(title, sub, body):
    return (f"<!doctype html><html><head><meta charset='utf-8'><title>{title}</title><style>{CSS}</style>"
            f"</head><body><div class='wrap'><h1>{title}</h1><div class='sub'>{sub}</div>"
            f"<pre>{body}</pre></div></body></html>")


def section(text, start, end=None):
    m = re.search(rf"(=+ {re.escape(start)}.*?)(?=\n=+ {re.escape(end)}|\Z)" if end
                  else rf"(=+ {re.escape(start)}.*)", text, re.S)
    return m.group(1) if m else text


def shoot(html_path, png_path, w, h):
    if not CHROME:
        print("未找到 Chrome，跳过截图：", html_path)
        return
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--window-size={w},{h}",
                    f"--screenshot={png_path}", html_path.as_uri()], check=True, capture_output=True, timeout=60)
    print("截图:", png_path)


def main():
    text = (EVID / "verify-output.txt").read_text(encoding="utf-8")
    jobs = [
        ("verify-report", "hw7 Redis 缓存 + Sentinel 限流验证（全部用例）", "scripts/hw7_verify.sh 实际输出", text, 1200, 1900),
        ("ratelimit-429", "hw7 并发压测：GET /api/v1/book/{id} 超出 5 QPS 返回 HTTP 429",
         "scripts/hw7_loadtest.py 并发压测实际输出（Sentinel 限流规则 QPS ≤ 5）",
         section(text, "5. Sentinel", "6."), 1200, 740),
        ("cache-redis", "hw7 Redis Cache-Aside / 防穿透 / 写后删缓存",
         "scripts/hw7_verify.sh 实际输出（用例 1~4）", section(text, "1. Cache-Aside", "5. Sentinel"), 1200, 720),
    ]
    for name, title, sub, body, w, h in jobs:
        p = EVID / f"{name}.html"
        p.write_text(page(title, sub, colorize(body)), encoding="utf-8")
        shoot(p, EVID / f"{name}.png", w, h)


if __name__ == "__main__":
    main()
