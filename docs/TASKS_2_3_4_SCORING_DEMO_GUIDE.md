# Task 2、3、4 评分点：实现文件、演示方法与提交证据

本文逐项对应 15 个评分点，说明应打开哪些文件、展示哪些代码、运行后看什么。命令以本项目 macOS zsh/bash 为准，从 `/Users/zhuolifeng/work_dir/Web` 执行。

完整启动步骤、curl 命令和预期结果见 [网页与 curl 演示命令](TASKS_2_3_4_WEB_DEMO_COMMANDS.md)：Task 2 对应 3.1–3.7，Task 3 对应 4.1–4.8，Task 4 对应 5.1–5.5。本文中的“预期”需以实际运行结果核对；已有证据不会随代码或服务变化自动更新。评分最终由评阅者判定。

## 1. Task 2：Function Calling 客服（5 分）

主要实现集中在 [assistant/server.py](../assistant/server.py)。前端展示位于 [AssistantPage.js](../frontend/src/pages/AssistantPage.js)，访问 `http://localhost:3000/assistant`。

### A. 成功定义符合模型要求的 Function Schema（1 分）

- **打开文件**：[assistant/server.py](../assistant/server.py)，定位 `TOOLS`，以及 `OpenAIModel.complete()`。
- **展示代码**：两个工具 `check_inventory`、`get_competitor_price` 各自的 `name`、`description`；参数对象的 `type: object`；`isbn` 的 `type: string` 和描述；`required: ['isbn']`、`additionalProperties: false`。`strict: true` 声明严格参数结构，运行时仍由本地代码校验。
- **说明接入**：`OpenAIModel.complete()` 将 `tools=TOOLS` 和 `tool_choice='auto'` 随 `messages` 发给模型，工具定义实际参与请求。
- **演示证据**：打开源码，并运行正常查询，展示模型返回的两个工具调用。仅展示 Schema 文本不能单独证明完整调用链已经运行。

### B. 正确接收并解析模型 Tool Call（1 分）

- **打开文件**：[assistant/server.py](../assistant/server.py)，定位 `OpenAIModel.complete()`、`run_chat()`、`execute_tool()`。
- **展示代码**：从模型响应取得 `message`，读取 `tool_calls`；校验调用类型、ID、函数名和参数；使用 `json.loads(arguments)` 解析模型给出的 JSON 参数字符串；通过 `FUNCTIONS` 白名单派发。
- **演示操作**：查询“帮我查一下 ISBN为12345 的书还有多少本，顺便看看别家卖多少钱。”
- **查看结果**：网页展开“工具调用记录”，或查看 curl 响应的 `data.toolCalls`，展示每项 `id`、`name`、`arguments`。服务终端同时打印 `TOOL_CALL`。
- **已有证据**：[live-normal.json](../assistant/live-normal.json)。

### C. 正确实现本地函数的模拟调用逻辑（1 分）

- **打开文件**：[assistant/server.py](../assistant/server.py)，定位 `DEMO_BOOKS`、`check_inventory()`、`competitor_fixture()`、`get_competitor_price()`、`Handler.do_GET()`。
- **展示代码**：库存演示模式从 `DEMO_BOOKS` 读取；竞品函数实际发送 HTTP 请求到本地 `/mock/competitor-price`，由模拟接口返回报价。
- **启动方式**：使用真实模型与演示库存：

```bash
INVENTORY_MODE=demo python3 scripts/run_assistant_live.py
```

- **查看结果**：查询 `12345`，库存为 100 本，模拟竞品价格为 55.90 元。展示工具结果中的数据来源和模拟标志。
- **范围说明**：`INVENTORY_MODE=demo` 不需要数据库；`live` 库存模式调用原 Java 后端。正式展示 Function Calling 时，模型应为真实 API 模式，不能将完全离线的 `DemoModel` 当作真实模型调用证据。

### D. 将函数结果回传模型上下文并生成最终回答（1 分）

- **打开文件**：[assistant/server.py](../assistant/server.py)，定位 `run_chat()`。
- **展示代码顺序**：

```python
messages.append(reply)  # 保留模型发出的 assistant 工具调用消息
messages.append({
    'role': 'tool',
    'tool_call_id': call['id'],
    'content': json.dumps(result, ensure_ascii=False)
})
```

- **说明闭环**：后续循环再次调用 `model.complete(messages)`；`tool_call_id` 关联原工具调用，模型读到真实执行结果后输出最终文本，程序返回 `answer`。如果模型继续要求工具，则继续处理，最多 5 轮。
- **演示证据**：同一正常案例同时展示 `data.toolCalls` 的库存、价格结果和 `data.answer` 的自然语言回答；不要只展示本地函数返回值。
- **已有证据**：[live-normal.json](../assistant/live-normal.json)。

### E. 异常处理及 Self-Correction（1 分）

- **打开文件**：[assistant/server.py](../assistant/server.py)，定位 `ServiceError`、`validate_isbn()`、`http_json()`、`execute_tool()`、`SYSTEM` 和 `run_chat()` 的超时次数限制。
- **展示机制**：错误被包装为包含 `code`、`message`、`retryable` 的工具结果，回传模型；模型可根据反馈修正参数、重试或询问用户。服务限制同一调用的超时重试，并设置总轮数上限。
- **错误 ISBN 演示**：新对话查询“查一下 ISBN为abc 的库存和竞品价格。”应要求正确 ISBN，不能编造库存。
- **超时演示**：新对话查询“查一下 ISBN为00000 的库存和竞品价格。”展示库存 12 本、竞品 `TIMEOUT`、最多重试一次，以及最后说明报价暂不可用。
- **已有证据**：[live-invalid.json](../assistant/live-invalid.json)、[live-timeout.json](../assistant/live-timeout.json)；参数错误反馈后修正的测试见 [test_assistant.py](../tests/assistant/test_assistant.py) 中的 `CorrectingModel`。
- **证据边界**：已有错误 ISBN 实测中，模型在工具调用前就识别格式错误，工具调用为 0 次。它证明合理询问，不证明“收到工具错误后修正”。展示真实错误反馈闭环时，优先使用超时案例；测试替身验证的修正过程应明确标为自动测试。

## 2. Task 3：RAG + ReAct 导购（5 分）

主文件为 [guide/rag.py](../guide/rag.py)、[guide/agent.py](../guide/agent.py)。网页位于 [GuidePage.js](../frontend/src/pages/GuidePage.js)，访问 `http://localhost:3000/guide`。

### A. 文档加载、切分与 Embedding 化（1 分）

- **打开文件**：政策源文档 [store_policy.md](../guide/data/store_policy.md)；[guide/rag.py](../guide/rag.py) 的 `build_policy_index()`、`chunk_document()`、`VectorIndex`。
- **展示代码流程**：读取 UTF-8 文档 → 按政策章节切分 → 最长 280 字符、重叠 50 字符 → 保留片段 ID 和来源 → 构建词表和 IDF → 计算 TF-IDF 向量并做 L2 归一化。
- **执行命令**：

```bash
python3 scripts/build_policy_index.py
```

- **查看产物**：[policy_index.json](../guide/data/policy_index.json)，包含 `embedding`、`chunkCount`、`dimensions`、`vocabulary`、`idf`、`chunks` 和 `vectors`。当前文档预期得到 8 个片段、1045 维；文档变动后以实际输出为准。
- **网页/curl 展示**：网页点击“查看政策分块与向量信息”；或调用 `GET http://127.0.0.1:8002/api/v1/guide/rag`。
- **范围说明**：当前 Embedding 是本地 TF-IDF 词项向量，算法名为 `local-tfidf-char-unigram-bigram-l2-v1`，没有调用神经 Embedding API。若课程明确要求特定语义 Embedding 模型，需要另行满足该要求，不能将本实现描述为该模型。

### B. 构建可用的 RAG 检索工具（1 分）

- **打开文件**：[guide/agent.py](../guide/agent.py) 的 `query_store_policy()`；[guide/rag.py](../guide/rag.py) 的 `VectorIndex.embed()`、`search()`。
- **展示机制**：用户问题使用政策索引相同的词表和 IDF 转成向量；计算归一化向量点积，即余弦相似度；取最多 3 个结果，过滤低于 0.12 的分数；返回正文、片段 ID、来源和分数。
- **演示问题**：“我想退货，拆了塑封之后发现不喜欢内容，还能退吗？”
- **查看结果**：`query_store_policy` 的 Observation 中有 `matches`，每项包含 `id`、`text`、`score`；最终回答使用实际检索片段的引用。
- **已有证据**：[live-return-case.json](../guide/evidence/live-return-case.json)、[live-return-case.log](../guide/evidence/live-return-case.log)。需要展示问题向量时，使用命令文档 4.6 的本地演示。

### C. 完整 ReAct 循环或状态图（1 分）

- **打开文件**：[guide/agent.py](../guide/agent.py) 的 `run_agent()`、`parse_step()`、`execute_action()` 和 `log_step()`。
- **展示循环**：

```text
模型选择 thought / action / action_input
  → 校验输出结构
  → 执行查书或政策检索工具
  → 记录真实 observation
  → 将 TOOL_OBSERVATION 加入模型上下文
  → 再次决策，直到 finish
```

- **展示完整性**：处理格式错误反馈、重复工具调用、无效参数、最终政策引用检查，并设置最多 7 步，防止无限循环。
- **实现方式说明**：这是自行实现的 ReAct 循环，不依赖 LangGraph，也没有声称使用 State Graph。`thought` 是公开的简短行动理由，不是模型内部隐藏思维链。
- **演示证据**：网页执行记录、curl 的 `data.trace`，以及 [live-case.log](../guide/evidence/live-case.log)。

### D. 按用户意图先后调用查书与查政策工具（1 分）

- **打开文件**：[guide/agent.py](../guide/agent.py) 的 `SYSTEM`、`TOOLS`、`search_book_catalog()`、`query_store_policy()` 和 `run_agent()`；模拟书目在 [catalog.json](../guide/data/catalog.json)。
- **展示路由设计**：系统提示要求复合问题先查书、再查政策；模型在每一步决定 action，应用按实际 action 执行。不能仅展示提示词就认定路由已经成功，需检查运行轨迹。
- **演示问题**：“我想买一本关于微服务的书，另外如果我买了不喜欢，拆了塑封还能退吗？”
- **查看顺序**：`search_book_catalog` → `query_store_policy` → `finish`。核对前一个工具有真实结果后才进行下一步。
- **已有证据**：[live-case.json](../guide/evidence/live-case.json)、[live-case.log](../guide/evidence/live-case.log)。可再展示单问退货只查政策，说明工具选择会随意图变化。

### E. 日志清晰展示 Thought → Action → Observation（1 分）

- **打开文件**：[guide/agent.py](../guide/agent.py) 的 `log_step()`，每步打印步骤号、Thought、Action（含参数）和 Observation。
- **已有可提交日志**：[live-case.log](../guide/evidence/live-case.log)、[live-return-case.log](../guide/evidence/live-return-case.log)。对应 JSON 保存结构化轨迹和最终回答。
- **终端查看**：

```bash
cat guide/evidence/live-case.log
```

- **重新生成真实模型案例日志**：

```bash
python3 scripts/run_guide_case.py --live
```

- **截图要点**：保留真实模型模式信息，以及连续步骤的 Thought、Action、Observation，尤其展示先查书、再查政策、最后 finish。
- **范围说明**：该命令会重新运行一次案例，按提示提供模型密钥；它不是导出已有网页会话。`demo-case.log` 属于离线替身演示，不能冒充真实模型日志。

## 3. Task 4：REST、虚拟线程与 JWT（5 分）

Java 主源码目录为 [com/bookstore/cloud](../cloud-backend/src/main/java/com/bookstore/cloud)，配置位于 [application.properties](../cloud-backend/src/main/resources/application.properties)。

### A. RESTful 接口设计（1 分）

- **打开文件**：[BookController.java](../cloud-backend/src/main/java/com/bookstore/cloud/BookController.java)、[BookCatalog.java](../cloud-backend/src/main/java/com/bookstore/cloud/BookCatalog.java)、[ApiResponse.java](../cloud-backend/src/main/java/com/bookstore/cloud/ApiResponse.java)、[ApiExceptionHandler.java](../cloud-backend/src/main/java/com/bookstore/cloud/ApiExceptionHandler.java)。
- **展示设计**：`/api/books/{id}` 使用名词标识图书资源；`@GetMapping` 表达读取，查询不修改业务数据；返回 JSON。真实 HTTP 状态为成功 200、非法 ID 400、不存在 404、未认证 401，而非所有情况都返回 200。
- **演示操作**：带有效 JWT 查询 `/api/books/1`、`/api/books/0`、`/api/books/99999`，核对 HTTP 状态。
- **范围说明**：这是遵循主要 REST 原则的查询 API，尚未实现超媒体链接或专门的图书缓存策略。JSON 的 `code` 不是 HTTP 状态码的替代品。

### B. 开启 Java 21 虚拟线程处理请求（1 分）

- **打开文件**：[pom.xml](../cloud-backend/pom.xml) 使用 Spring Boot 3.2.5、Java 编译目标 21；[application.properties](../cloud-backend/src/main/resources/application.properties) 默认配置：

```properties
spring.threads.virtual.enabled=${VIRTUAL_THREADS_ENABLED:true}
```

- **展示慢 I/O**：[BookCatalog.java](../cloud-backend/src/main/java/com/bookstore/cloud/BookCatalog.java) 的 `Thread.sleep(delayMs)` 默认等待 200ms。
- **展示运行证据**：[RequestThreadFilter.java](../cloud-backend/src/main/java/com/bookstore/cloud/RequestThreadFilter.java) 在实际请求线程上调用 `Thread.currentThread().isVirtual()`，成功请求响应头包含 `X-Request-Thread-Virtual: true`。
- **对照测试**：[PlatformThreadControlTest.java](../cloud-backend/src/test/java/com/bookstore/cloud/PlatformThreadControlTest.java) 关闭虚拟线程，验证标志变为 false；[CloudApiTest.java](../cloud-backend/src/test/java/com/bookstore/cloud/CloudApiTest.java) 检查并发请求使用虚拟线程。
- **版本说明**：本机文档记录的实际验证使用 Java 24，源码目标和功能基线为 Java 21。截图按 `X-Java-Version` 如实展示；如果必须使用 JDK 21 运行，需切到 JDK 21 后重新验收。虚拟线程改善并发等待能力，不会缩短 sleep 本身，也不能仅凭配置断言生产吞吐提升。

### C. 集成 JWT 库并生成 Token（1 分）

- **打开文件**：[pom.xml](../cloud-backend/pom.xml) 的 JJWT 0.12.5 依赖 `jjwt-api`、`jjwt-impl`、`jjwt-jackson`；[AuthController.java](../cloud-backend/src/main/java/com/bookstore/cloud/AuthController.java)；[JwtService.java](../cloud-backend/src/main/java/com/bookstore/cloud/JwtService.java)。
- **展示流程**：`POST /api/auth/token` → 校验演示账号密码 → `jwt.issue(username)` → `Jwts.builder()` 添加发行方、用户名、角色、签发时间和过期时间 → HS256 签名 → `compact()` 输出 JWT。
- **关键点**：签名密钥来自环境配置；JJWT 自动完成 Base64URL 编码。编码不是加密，JWT 内容不应存密码。
- **演示操作**：命令文档 5.5 的登录命令将 `data.accessToken` 保存到终端变量 `TOKEN`，再用它成功查询图书，证明生成的 Token 可用。避免把真实 Token 展开到截图中。

### D. 无状态鉴权拦截器/过滤器（1 分）

- **打开文件**：[JwtAuthenticationFilter.java](../cloud-backend/src/main/java/com/bookstore/cloud/JwtAuthenticationFilter.java)、[JwtService.java](../cloud-backend/src/main/java/com/bookstore/cloud/JwtService.java)、[SecurityConfig.java](../cloud-backend/src/main/java/com/bookstore/cloud/SecurityConfig.java)。
- **展示过滤逻辑**：继承 `OncePerRequestFilter` → 读取 `Authorization` → 提取 Bearer Token → `jwt.verify()` 校验签名、过期时间、发行方和必要声明 → 设置本次请求认证上下文 → 继续访问 Controller。无效 Token 直接返回 HTTP 401。
- **展示注册与规则**：通过 `addFilterBefore()` 注册；登录接口允许匿名，`/api/books/**` 必须认证。未携带 Token 时由安全链拒绝，返回 401。
- **展示无状态配置**：`SessionCreationPolicy.STATELESS`、`NullSecurityContextRepository`，并禁用请求缓存；认证不保存到 Session。
- **演示证据**：有效 Token 成功后，下次不带 Token 仍为 401；只提供 Session Cookie 仍为 401。查看验收报告对应卡片及 [CloudApiTest.java](../cloud-backend/src/test/java/com/bookstore/cloud/CloudApiTest.java) 的无状态测试。

### E. 提供 401 拦截和 200 成功截图（1 分）

- **先启动服务**：

```bash
python3 cloud-backend/scripts/start.py
```

- **另开终端生成实际 HTTP 报告**：

```bash
python3 cloud-backend/scripts/verify_api.py
open cloud-backend/evidence/auth-report.html
```

- **截图内容**：前两张卡片的请求地址、无 Token 的 HTTP 401、有效 Token 的 HTTP 200、返回图书数据，并保留 `X-Request-Thread-Virtual: true` 和实际 Java 版本。
- **保存路径**：[auth-401-200.png](../cloud-backend/evidence/auth-401-200.png)。仓库已有该截图；重新运行后应人工核对并更新截图，生成报告不会自动更新 PNG。
- **原始结果**：[auth-results.json](../cloud-backend/evidence/auth-results.json)；[auth-report.html](../cloud-backend/evidence/auth-report.html) 是可截图的报告，不是图片本身。
- **另一种展示**：按命令文档 5.5 使用 `curl -i`，将 401 和携带 Token 的 200 响应截在同一张终端截图中。若老师要求原始终端或 Network 证据，使用指定形式。
- **更新最终源码提交包**：

```bash
python3 cloud-backend/scripts/package_submission.py --require-screenshot
```

## 4. 推荐的现场展示顺序

| 任务 | 打开源码 | 运行展示 | 提交证据 |
| --- | --- | --- | --- |
| Task 2 | `TOOLS` → `run_chat` → `execute_tool` → 结果回传 | 正常编号 12345；错误 abc；超时 00000 | 三个 `assistant/live-*.json`，工具记录截图，异常测试说明 |
| Task 3 | `rag.py` → `query_store_policy` → `run_agent` → `log_step` | 构建索引；复合问题；退货单问 | 索引产物、`live-case.log/json`、`live-return-case.log/json`，链路截图 |
| Task 4 | Controller → 虚拟线程配置 → JWT 签发 → Filter 与安全配置 | 无 Token 401；有效 Token 200；线程标志 true；无状态检查 | `auth-401-200.png`、实际报告、自动测试摘要 |

Task 2/3 正式演示使用真实模型模式，书目、政策、竞品和演示库存的模拟属性需如实展示。学校模型 API 有每分钟 10 次请求限制，一个用户问题可能触发多次模型调用，应逐项演示，等待前一次结束后再发送下一题。
