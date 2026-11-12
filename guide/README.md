# Task 3：RAG + ReAct 图书导购 Agent

本任务新增 `/guide` 页面与独立的 Python 服务（8002 端口），实现模拟查书、政策检索和 ReAct 执行链。Task 2 的 `/assistant` 和 8001 服务保持原功能。

## 一、需要安装什么

**没有新增依赖。** Python 3.10+ 只用标准库；前端复用已经安装的 React / Ant Design。不会自动执行任何安装。

只有在新电脑还没有前端依赖时，才由你自行执行：

```bash
cd frontend
npm ci
```

本任务不需要 MySQL、Java 后端、向量数据库、Embedding 模型下载或 pip 安装。

## 二、先用离线模式检查基本功能

在原工程根目录执行：

```bash
python3 scripts/build_policy_index.py
python3 scripts/run_guide.py --demo
```

第一个命令加载 `guide/data/store_policy.md`，分块并向量化，生成可查看的 `guide/data/policy_index.json`。当前文档得到 8 个片段、1045 维向量。

第二个命令启动 `http://127.0.0.1:8002`。保持终端运行，停止时按 Ctrl+C。

另开终端启动原前端：

```bash
cd frontend
npm start
```

如果前端已经在 3000 端口运行，直接刷新即可，不要再启动一个实例。

打开：

```text
http://localhost:3000/guide
```

左侧菜单也有“图书导购 Agent”。点击“复杂案例”后发送，展开 Observation 与政策来源。离线模式使用确定性模型替身，不是实际外部大模型。

## 三、用真实模型验收

先在离线服务终端按 Ctrl+C，随后运行：

```bash
python3 scripts/run_guide.py
```

按照提示输入模型 API key，输入不回显、不写入文件。默认配置为：

```text
OPENAI_BASE_URL=https://models.sjtu.edu.cn/api/v1
OPENAI_MODEL=deepseek-chat
GUIDE_PORT=8002
```

刷新 `/guide`，确认显示“真实模型 ReAct Agent”。Task 2 若仍在 8001 运行无需停止，它与 Task 3 服务独立。

若需要修改公开配置，可以在启动前设置 `OPENAI_BASE_URL`、`OPENAI_MODEL`、`GUIDE_PORT`。前端地址由启动/构建时的 `REACT_APP_GUIDE_URL` 控制，默认 `http://localhost:8002`。更换前端端口时，设置 `FRONTEND_ORIGINS` 为对应 Origin，默认只允许 localhost:3000 与 127.0.0.1:3000。

## 四、手工测试清单

| 测试问题 | 应观察到的行为 |
| --- | --- |
| 我想买一本关于微服务的书，另外如果我买了不喜欢，拆了塑封还能退吗？ | 先 search_book_catalog，再 query_store_policy，最后 finish；推荐模拟微服务书目，说明仅因不喜欢的拆封图书不支持无理由退货，并引用 P02 片段 |
| 银卡会员有哪些优惠？积分怎么抵扣？ | 只检索政策，不需要查书；查看 P05/P06 片段，区分积分与银卡规则 |
| 我刚开始学习后端，推荐一本微服务入门书。 | 只查模拟目录，不必调用政策工具 |
| 图书拆封后发现缺页，还能退换吗？ | 检索质量售后 P03，不能把质量问题误认为个人不喜欢 |
| 政策里对量子色动力学有什么规定？ | 对不相关问题说明缺少政策依据，不编造条款 |

页面直接显示 Thought（简短行动理由）、Action（工具与参数），以及可展开的 Observation（实际执行结果）。Thought 不是模型内部隐藏思维链。

点击“查看政策分块与向量信息”，检查片段 ID、文本、分块参数、向量维度与向量预览。向量是稀疏词项向量，某些预览分量为 0 属于正常现象；完整向量存于 policy_index.json。

服务端也会打印以下执行链：

```text
Thought: 先查找相关书目。
Action: search_book_catalog {"query":"微服务架构"}
Observation: 实际模拟书目结果
Thought: 再检索拆封退货规则。
Action: query_store_policy {"query":"拆封 不喜欢 退货"}
Observation: 政策片段、相似度、出处
Thought: 整理有依据的回答。
Action: finish {}
Observation: 最终回答
```

以上只是格式示意。实际执行日志放在 `guide/evidence/`。

## 五、不打开网页也能生成验收日志

完全离线：

```bash
python3 scripts/run_guide_case.py
```

真实模型：

```bash
python3 scripts/run_guide_case.py --live
```

脚本直接运行 Agent，不需要先启动 8002 服务。结束后自动退出，不留下后台服务。分别生成：

```text
guide/evidence/demo-case.json
guide/evidence/demo-case.log
guide/evidence/live-case.json
guide/evidence/live-case.log
```

可替换问题，例如：

```bash
python3 scripts/run_guide_case.py --live --question '银卡会员积分如何抵扣？' --output guide/evidence/live-membership.json
```

学校接口每分钟最多 10 次模型请求，复杂案例通常需要 3 次，错误重试可能增加请求数。请一次测试一个问题，遇到限流等待后再试；不要并发快速运行全部场景。

## 六、自动测试

在原工程根目录运行：

```bash
sh scripts/test_guide.sh
```

脚本依次执行 Python 测试、Task 3 与 Task 2 页面测试、前端生产构建，不执行依赖安装，也不访问收费模型。

单独运行 Python：

```bash
python3 -m unittest discover -s tests/guide -v
```

测试包含本机临时 HTTP 服务，需要本机允许监听端口。具体已执行结果见 [VALIDATION.md](VALIDATION.md)。

## 七、实现说明与限制

完整过程和评分项对应见 [TASK3_IMPLEMENTATION.md](TASK3_IMPLEMENTATION.md)。

- 分块：按政策章节切分，再使用最多 280 字、重叠 50 字的窗口；当前各章节较短，各产生一个片段，长文本窗口逻辑另有测试。
- Embedding：本地中文单字/双字及英文词项 TF-IDF，经过 L2 归一化，是词项向量基线，不是神经语义模型，也没有调用 Embedding API。
- 检索：余弦相似度，最多返回 3 个片段，最低分 0.12；低于阈值不作为政策依据。
- Agent：模型每轮输出一个 JSON 决策，应用执行动作并追加真实 Observation，再请求模型；最多 7 轮，拒绝未知动作和重复调用。
- 引用：最终回答必须引用检索到的政策 ID；程序拒绝未检索到的引用。引用存在不等于自动证明每句话正确，验收时仍需核对原文。
- 目录、价格和政策均为课程模拟，不能用作真实店铺承诺。知识库只在进程启动后首次加载时建立索引；修改政策后重启导购服务即可更新。

## 八、提交什么

```bash
python3 scripts/package_guide_submission.py
```

上传生成的 `submission/ebookstore-task3-submission.zip`。它只包含本次源码、模拟数据、向量产物、脚本、文档、测试与执行证据，不包含整个工程、node_modules、build、target、JAR、.env、.git 或真实 API key。

这是原 E-BookStore 工程的增量提交包，请在原工程根目录解压覆盖使用。包含 `assistant/server.py` 作为本次复用的自编 HTTP 助手，以及当前路由所需的 Task 2 页面，避免覆盖到原工程后出现缺失导入；这些是自编源码，不是第三方依赖。

建议在课程平台补充三张截图：复杂问题与最终回答、展开的 Thought/Action/Observation、政策分块与检索引用。截图中不要包含密钥。
