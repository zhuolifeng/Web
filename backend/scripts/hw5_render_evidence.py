#!/usr/bin/env python3
"""把 hw5 演示的真实输出渲染成 HTML，并用本机 Chrome headless 截图到 hw5-evidence/*.png。

用法：python3 scripts/hw5_render_evidence.py
只读取 hw5-evidence/demo-output.txt 与 hw5-evidence/backend-console.log，不访问后端、不修改数据。
"""
import html
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EVID = ROOT / "hw5-evidence"
CHROME = next((c for c in ["/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                           shutil.which("google-chrome") or "", shutil.which("chromium") or ""]
               if c and Path(c).exists()), None)

CSS = """
body{margin:0;background:#0f1115;color:#d7dae0;font:13px/1.5 Menlo,Consolas,monospace}
h1{font:600 18px -apple-system,sans-serif;margin:0 0 4px;color:#fff}
h2{font:600 14px -apple-system,sans-serif;margin:16px 0 6px;color:#c084fc}
.sub{color:#8b93a1;font:12px -apple-system,sans-serif;margin-bottom:14px}
.wrap{padding:18px 22px}
pre{white-space:pre-wrap;word-break:break-all;margin:0;background:#161a21;border:1px solid #262c36;border-radius:6px;padding:12px}
.sec{color:#c084fc;font-weight:700}.ok{color:#4ade80;font-weight:700}.bad{color:#f87171;font-weight:700}
.acc{color:#fbbf24;font-weight:700}.proc{color:#60a5fa}.dim{color:#6b7280}
"""

HEADER_NOISE = re.compile(r"^(Vary|X-Content-Type-Options|X-XSS-Protection|Cache-Control|Pragma|Expires|"
                          r"X-Frame-Options|Transfer-Encoding|Date):")


def colorize(text: str) -> str:
    out = []
    for raw in text.splitlines():
        if HEADER_NOISE.match(raw):
            continue
        line = html.escape(raw)
        if raw.startswith("####") or raw.startswith("== "):
            line = f'<span class="sec">{line}</span>'
        elif "HTTP/1.1 202" in raw or "POST 耗时" in raw or "QUEUED" in raw:
            line = f'<span class="acc">{line}</span>'
        elif "SUCCESS" in raw:
            line = f'<span class="ok">{line}</span>'
        elif "FAILED" in raw:
            line = f'<span class="bad">{line}</span>'
        elif "PROCESSING" in raw or "已投递" in raw or "消费订单事件" in raw:
            line = f'<span class="proc">{line}</span>'
        out.append(line)
    return "\n".join(out)


def page(title, sub, blocks):
    body = "".join((f"<h2>{html.escape(h)}</h2>" if h else "") + f"<pre>{colorize(t)}</pre>" for h, t in blocks)
    return (f"<!doctype html><html><head><meta charset='utf-8'><title>{title}</title><style>{CSS}</style>"
            f"</head><body><div class='wrap'><h1>{title}</h1><div class='sub'>{sub}</div>{body}</div></body></html>")


def section(text, start, end=None):
    i = text.find(start)
    if i < 0:
        return text
    j = text.find(end, i + len(start)) if end else -1
    return text[i:j if j > 0 else None].rstrip()


def shoot(html_path, png_path, w, h):
    if not CHROME:
        print("未找到 Chrome，跳过截图：", html_path)
        return
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--window-size={w},{h}",
                    f"--screenshot={png_path}", html_path.as_uri()], check=True, capture_output=True, timeout=60)
    print("截图:", png_path)


def main():
    demo = (EVID / "demo-output.txt").read_text(encoding="utf-8")
    log = (EVID / "backend-console.log").read_text(encoding="utf-8")
    log = re.sub(r"^(\S+)-08:00\s+INFO \d+ --- ", r"\1 ", log, flags=re.M)
    s1 = section(demo, "######## 场景 1", "######## 场景 2")
    s2 = section(demo, "######## 场景 2", "######## Kafka")
    topic = section(demo, "######## Kafka")
    jobs = [
        ("01-success-flow", "hw5 正常下单：POST /api/orders 立即 202，SSE 约 3 秒后推送 SUCCESS",
         "scripts/demo_async_order.sh 实际输出（BOOK_ID=1 QTY=1）", [("", s1)], 1300, 900),
        ("02-failed-flow", "hw5 库存不足：消费者处理失败，SSE 推送 FAILED",
         "scripts/demo_async_order.sh 实际输出（BOOK_ID=2 QTY=9999）", [("", s2)], 1300, 900),
        ("03-kafka-and-console", "hw5 Kafka 生产/消费 + SSE 回调（后端控制台日志与 topic 消息）",
         "KafkaProducer 投递 → @KafkaListener 消费（独立线程）→ OrderSseHub 推送；"
         "注意 HTTP 线程 (http-nio-8080-exec) 与消费线程 (ntainer#0-0-C-1) 不同",
         [("后端控制台日志", log), ("kafka-console-consumer.sh --topic order-events", topic)], 1500, 820),
        ("00-full-report", "hw5 Kafka 异步下单 + SSE 实时推送：完整流程",
         "成功 / 失败两个场景 + Kafka topic 消息 + 后端日志",
         [("", demo), ("后端控制台日志", log)], 1400, 2600),
    ]
    for name, title, sub, blocks, w, h in jobs:
        p = EVID / f"{name}.html"
        p.write_text(page(title, sub, blocks), encoding="utf-8")
        shoot(p, EVID / f"{name}.png", w, h)


if __name__ == "__main__":
    main()
