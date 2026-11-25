#!/usr/bin/env python3
"""把 run_all.sh 记录的真实 mysql 输出渲染为 HTML，并用 Chrome headless 截图到 evidence/*.png。
只读取 evidence/explain-output.txt，不连接数据库。"""
import html
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EVID = ROOT / "evidence"
CHROME = next((c for c in ["/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                           shutil.which("google-chrome") or "", shutil.which("chromium") or ""]
               if c and Path(c).exists()), None)

CSS = """
body{margin:0;background:#0f1115;color:#d7dae0;font:12px/1.45 Menlo,Consolas,monospace}
h1{font:600 18px -apple-system,sans-serif;margin:0 0 4px;color:#fff}
.sub{color:#8b93a1;font:12px -apple-system,sans-serif;margin-bottom:14px}
.wrap{padding:18px 22px}
pre{white-space:pre;margin:0;background:#161a21;border:1px solid #262c36;border-radius:6px;padding:12px;overflow:hidden}
.sec{color:#c084fc;font-weight:700}.sql{color:#60a5fa}
.w{white-space:pre-wrap;word-break:break-all;display:inline-block;max-width:100%}
.good{background:#14532d;color:#86efac;font-weight:700}.bad{background:#5f1d1d;color:#fca5a5;font-weight:700}
.key{background:#3b2f0b;color:#fde68a;font-weight:700}
"""

GOOD = [r"Using index condition", r"Using where; Using index", r"Using index(?! condition)",
        r"Index range scan[^\n|]*", r"Covering index[^\n|]*"]
BAD = [r"Using filesort", r"Using temporary", r"Table scan[^\n|]*"]


def mark(line: str) -> str:
    if line.startswith("========") or line.startswith("####"):
        return f'<span class="sec">{line}</span>'
    if line.startswith("mysql&gt;") or line.startswith("-- "):
        return f'<span class="sql w">{line}</span>'
    for p in GOOD:
        line = re.sub(p, lambda m: f'<span class="good">{m.group(0)}</span>', line)
    for p in BAD:
        line = re.sub(p, lambda m: f'<span class="bad">{m.group(0)}</span>', line)
    # EXPLAIN 表格行：type 列 range/ref 为好，ALL 为坏；key 列 idx_* 高亮
    if line.startswith("|") and "SIMPLE" in line:
        # 只标 type 列（每行第一个匹配），不误标 ref 列中的 const
        line = re.sub(r"\| (range|ref|eq_ref|const|ALL) ", lambda m: (
            f'| <span class="{"bad" if m.group(1) == "ALL" else "good"}">{m.group(1)}</span> '), line, count=1)
        line = re.sub(r"\| (idx_\w+|PRIMARY) ", r'| <span class="key">\1</span> ', line)
    if not line.startswith(("|", "+")):
        line = f'<span class="w">{line}</span>'
    return line


def page(title, sub, text):
    body = "\n".join(mark(l) for l in html.escape(text).splitlines())
    return (f"<!doctype html><html><head><meta charset='utf-8'><title>{title}</title><style>{CSS}</style></head>"
            f"<body><div class='wrap'><h1>{title}</h1><div class='sub'>{sub}</div><pre>{body}</pre></div></body></html>")


def section(text, start, end=None):
    i = text.find(f"======== {start}")
    j = text.find(f"======== {end}") if end else -1
    return text[i:j if j > 0 else None].rstrip() if i >= 0 else text


def shoot(name, title, sub, text, w):
    p = EVID / f"{name}.html"
    p.write_text(page(title, sub, text), encoding="utf-8")
    if not CHROME:
        print("未找到 Chrome，跳过截图：", p)
        return
    h = 140 + 17.5 * sum(1 + len(l) // 190 for l in text.splitlines())
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--window-size={w},{int(h)}",
                    f"--screenshot={EVID / (name + '.png')}", p.as_uri()], check=True, capture_output=True, timeout=60)
    print("截图:", EVID / f"{name}.png")


def main():
    t = (EVID / "explain-output.txt").read_text(encoding="utf-8")
    sub = "scripts/run_all.sh 在本机 MySQL 上的实际输出（bookstore_hw8，books 10 万行）"
    shoot("01-explain-before-after", "hw8 EXPLAIN：建复合索引前后对比", sub,
          section(t, "1.", "4.") , 1700)
    shoot("02-explain-covering-order-leftmost", "hw8 EXPLAIN：覆盖索引 / 排序分页 / 最左前缀反例", sub,
          section(t, "4.", "7."), 1700)
    shoot("03-explain-analyze", "hw8 EXPLAIN ANALYZE：走索引 vs 全表扫描实际耗时", sub,
          section(t, "7.", "8."), 1700)
    shoot("04-denormalization", "hw8 反范式：订单列表 JOIN vs 单表冗余字段", sub,
          section(t, "8."), 1700)
    shoot("00-full-report", "hw8 全部输出", sub, t, 1700)


if __name__ == "__main__":
    main()
