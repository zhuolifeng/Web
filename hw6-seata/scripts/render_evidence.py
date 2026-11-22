#!/usr/bin/env python3
"""把 verify.sh 的真实输出渲染成 HTML，并用本机 Chrome headless 截图（evidence/*.png）。

用法：python3 scripts/render_evidence.py
仅读取 evidence/verify-output.txt 和 evidence/rollback-console.log，不修改任何数据。
"""
import html
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EVID = ROOT / "evidence"
CHROME_CANDIDATES = [
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    shutil.which("google-chrome") or "",
    shutil.which("chromium") or "",
]

CSS = """
body{margin:0;background:#0f1115;color:#d7dae0;font:13px/1.45 Menlo,Consolas,monospace}
h1{font:600 18px -apple-system,sans-serif;margin:0 0 4px;color:#fff}
.sub{color:#8b93a1;font:12px -apple-system,sans-serif;margin-bottom:14px}
.wrap{padding:18px 22px}
pre{white-space:pre-wrap;word-break:break-all;margin:0;background:#161a21;border:1px solid #262c36;
    border-radius:6px;padding:12px}
.pass{color:#4ade80;font-weight:700}.fail{color:#f87171;font-weight:700}
.case{color:#fbbf24;font-weight:700}.err{color:#f87171}.rb{color:#60a5fa;font-weight:700}
.hl{background:#3b2f0b;color:#fde68a}.sec{color:#c084fc;font-weight:700}
"""


def colorize(text: str) -> str:
    out = []
    for line in html.escape(text).splitlines():
        if "[PASS]" in line:
            line = f'<span class="pass">{line}</span>'
        elif "[FAIL]" in line:
            line = f'<span class="fail">{line}</span>'
        elif line.startswith("####") or line.startswith("=====") or line.startswith("# "):
            line = f'<span class="case">{line}</span>'
        elif line.startswith("----") or line.startswith("=========="):
            line = f'<span class="sec">{line}</span>'
        elif " ERROR " in line or "模拟订单入库异常" in line:
            line = f'<span class="err">{line}</span>'
        elif re.search(r"Rollbacked|rollback success|Rollback global|rollback status|will be rollback", line):
            line = f'<span class="rb">{line}</span>'
        out.append(line)
    return "\n".join(out)


def page(title: str, sub: str, body: str) -> str:
    return (f"<!doctype html><html><head><meta charset='utf-8'><title>{title}</title>"
            f"<style>{CSS}</style></head><body><div class='wrap'><h1>{title}</h1>"
            f"<div class='sub'>{sub}</div><pre>{body}</pre></div></body></html>")


def extract_case2(text: str) -> str:
    """只保留用例 2（异常回滚）的数据库前后对比。"""
    m = re.search(r"(#+ 用例 2.*?)(?=\n#+ 对照组)", text, re.S)
    return m.group(1) if m else text


def shoot(html_path: Path, png_path: Path, width: int, height: int) -> bool:
    chrome = next((c for c in CHROME_CANDIDATES if c and Path(c).exists()), None)
    if not chrome:
        print("未找到 Chrome，跳过截图；可手动打开", html_path)
        return False
    subprocess.run([chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                    f"--window-size={width},{height}", f"--screenshot={png_path}",
                    html_path.as_uri()], check=True, capture_output=True, timeout=60)
    print("截图:", png_path)
    return True


def main():
    verify = (EVID / "verify-output.txt").read_text(encoding="utf-8")
    logs = (EVID / "rollback-console.log").read_text(encoding="utf-8")

    db_html = EVID / "db-before-after.html"
    db_html.write_text(page("hw6 异常流程：数据库前后状态对比",
                            "bookstore_inventory.inventory / bookstore_order.orders / undo_log —— 来自 scripts/verify.sh 实际输出",
                            colorize(extract_case2(verify))), encoding="utf-8")
    log_html = EVID / "rollback-console.html"
    log_html.write_text(page("hw6 异常流程：Seata 全局回滚控制台日志",
                             "order-service(TM/RM) · inventory-service(RM) · seata-server(TC) —— 来自 logs/*.log 实际输出",
                             colorize(logs)), encoding="utf-8")
    full_html = EVID / "verify-report.html"
    full_html.write_text(page("hw6 Seata AT 分布式事务验证（全部用例）", "scripts/verify.sh 实际输出",
                              colorize(verify)), encoding="utf-8")

    shoot(db_html, EVID / "db-before-after.png", 1100, 1250)
    shoot(log_html, EVID / "rollback-console.png", 2000, 1150)
    shoot(full_html, EVID / "verify-report.png", 1100, 3900)


if __name__ == "__main__":
    main()
