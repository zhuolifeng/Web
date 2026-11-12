# Task 3 实际执行结果

校验日期：2026-10-24。本文区分实际外部模型、本机测试替身与离线演示，不将模拟书目或模拟政策当作真实书店数据。

## 实际执行的检查

| 检查 | 命令 / 方式 | 实际结果 |
| --- | --- | --- |
| 文档加载、分块与向量化 | python3 scripts/build_policy_index.py | 8 个片段、1045 维 TF-IDF 向量，保存完整向量产物 |
| Python 自动测试 | python3 -m unittest discover -s tests/guide -v | 25 项全部通过 |
| Task 3 页面测试 | React Jest / Testing Library | 5 项全部通过 |
| Task 2 页面回归 | 与 Task 3 页面测试一起执行 | 7 项全部通过，合计 12 项 |
| 前端生产构建 | npm run build | Compiled successfully |
| 复杂案例离线执行 | python3 scripts/run_guide_case.py | 三步执行，生成 demo-case.json 与 demo-case.log |
| 复杂案例真实模型执行 | python3 scripts/run_guide_case.py --live | 学校 deepseek-chat 成功完成三步执行，生成 live-case.json 与 live-case.log |
| 向量与案例产物复核 | python3 scripts/verify_guide_artifacts.py | 向量范数、维度、原文哈希、工具顺序、Observation、引用与日志均通过 |

生产构建有既有 Browserslist 数据过期提示，未执行更新或任何依赖安装，构建成功。

## 真实复杂案例的执行链

用户输入：

> 我想买一本关于微服务的书，另外如果我买了不喜欢，拆了塑封还能退吗？

实际模型：学校 API `deepseek-chat`，通过真实 HTTP 请求调用，没有使用离线决策替身。

1. 模型选择 `search_book_catalog`，参数 query 为“微服务架构 图书推荐”。工具返回两本模拟微服务图书，分别为 69.00 元与 89.00 元。
2. 模型看到书目 Observation 后选择 `query_store_policy`，参数 query 为“图书拆封塑封后不喜欢是否可以退货”。向量检索返回 P02-01、P01-01、P07-01，最高分为 P02-01。
3. 模型选择 finish，回答给出模拟图书推荐，说明拆封后仅因不喜欢不支持无理由退货，区分质量问题，并引用实际检索到的 P02-01 和 P01-01。

每步都有模型生成的简短行动理由、实际动作和程序产生的 Observation。完整原始结果请查看 evidence/live-case.json 与 evidence/live-case.log。

密钥由隐藏输入进入临时执行进程，未写入证据。该命令执行结束后已退出，没有为联调留下长期运行的 8002 服务。

## 证据边界

- 书目、作者、价格和政策是课程模拟内容；只有模型请求为真实外部 API。
- Embedding 是本地 TF-IDF 词项向量，不是神经语义 Embedding API，不需要模型下载或新增 Python 依赖。
- Python 的模型适配 HTTP 测试使用本机测试替身；真实 API 的验证以 live-case 证据为准。
- 本次用完整案例验证真实模型的查书→查政策路由；政策单问、书目单问、格式纠错、引用纠错与循环上限通过自动测试验证，没有声称全部场景均单独请求过外部模型。
- 前端交互通过自动测试和生产构建；没有另行完成自动浏览器截图验收，用户可按 README 自行检查并截图。
- 引用检查确保 ID 来自已检索片段，不等于逐句事实蕴含验证。词项检索对远距离语义改写能力有限，未匹配时应说明依据不足。

## 提交包范围

独立 ZIP 使用文件白名单，只包含自编源码、模拟数据、向量产物、测试、脚本、文档和执行日志，并生成 SHA-256 文件清单。包含复用的自编 HTTP 助手与当前前端路由必需的 Task 2 页面；不包含第三方库或整个工程。
