# Task 2、3、4 网页演示命令

适用目录：`/Users/zhuolifeng/work_dir/Web`。每个运行服务的终端都要保持打开；结束演示时，在对应终端按 **Control+C** 停止。

## 1. 三项演示的区别与端口

| 用途 | 端口 | 浏览器打开的位置 |
| --- | --- | --- |
| 共用 React 前端 | 3000 | Task 2、3 共用，只启动一次 |
| Task 2 库存/竞品客服服务 | 8001 | http://localhost:3000/assistant |
| Task 3 RAG/ReAct 导购服务 | 8002 | http://localhost:3000/guide |
| Task 4 JWT/虚拟线程 API | 8081 | 先执行验收脚本，再打开生成的 auth-report.html |

Task 2、3 是交互聊天页面。Task 4 是 API 验收报告页，没有对应的 React 聊天页面。

下面 Task 2 使用真实模型加演示库存；Task 3 使用真实模型加模拟目录和政策。两者不需要 MySQL。Task 4 不使用大模型 API，也不需要 MySQL。

## 2. 共用准备：启动 React 前端一次

打开一个终端，记作“终端 A”：

```bash
cd /Users/zhuolifeng/work_dir/Web/frontend
npm start
```

看到编译成功后保持运行。Task 2、3 都使用这个前端，不要为两个任务分别再运行一次 npm start。

如果前端已经在 3000 运行，直接访问或刷新下面的网页即可。如果提示切换端口，先选择 no，检查是否已有前端实例，避免地址与跨域配置不一致。

## 3. Task 2：Function Calling 客服网页演示

### 3.1 启动 Task 2 服务

另开“终端 B”：

```bash
cd /Users/zhuolifeng/work_dir/Web
INVENTORY_MODE=demo python3 scripts/run_assistant_live.py
```

出现 API key 提示时，输入学校模型服务的密钥。输入不回显，也不保存到源码。默认调用学校 API 的 deepseek-chat。

这里的组合是：

```text
真实大模型 API + 演示库存 + 模拟竞品 HTTP API
```

保持终端运行，服务默认监听 8001。

### 3.2 打开网页

```bash
open http://localhost:3000/assistant
```

也可以手动将地址复制到浏览器。

确认页面显示“真实模型 API 模式”，库存来源为“演示数据，不是实时库存”。

### 3.3 正常查询演示

点击“新对话”，输入并发送：

```text
帮我查一下 ISBN为12345 的书还有多少本，顺便看看别家卖多少钱。
```

展示：

- 自然语言回答：演示库存为 100 本，模拟竞品价格为 55.90 元。
- 展开“工具调用记录”，查看 check_inventory 和 get_competitor_price。
- 展示各自的 isbn 参数和执行结果。
- 终端 B 有 TOOL_CALL 日志；浏览器 Console 也有 Function Calling 记录。

12345 是程序接受的课堂演示编号，不是真实国际书号。

### 3.4 错误 ISBN 演示

点击“新对话”，输入：

```text
查一下 ISBN为abc 的库存和竞品价格。
```

预期模型说明格式错误并要求正确 ISBN，不编造库存。模型可能在调用工具前就识别错误，也可能收到 INVALID_ISBN 工具结果后询问；不要保证一定产生两个工具调用。

### 3.5 模拟超时演示

点击“新对话”，输入：

```text
查一下 ISBN为00000 的库存和竞品价格。
```

预期：库存工具返回 12 本；竞品工具超时并最多重试一次，随后模型说明暂时无法取得报价。展开记录展示 TIMEOUT 与已经成功的库存结果。

### 3.6 需要真实库存时

实时库存需要三个服务同时运行：MySQL（默认 3306）、原 Java 后端 `backend/`（8080）、Task 2 助手（8001）。下面按顺序启动。Task 4 的 `cloud-backend/`（8081）提供演示图书，不能替代这里的数据库后端。

#### 第一步：确认 MySQL 已启动并准备数据库

如果 MySQL 已在运行，可直接检查登录。若由 Homebrew 安装，先查看实际服务名：

```bash
brew services list
```

只有服务名确实为 `mysql` 且尚未启动时，执行：

```bash
brew services start mysql
```

若安装的是 `mysql@8.0` 等版本，使用列表中的实际服务名；通过其他方式安装的 MySQL，应使用对应方式启动。

使用本机数据库账号登录（下面以 root 为例，密码在提示后输入）：

```bash
mysql -u root -p
```

在 MySQL 提示符执行，已有数据库可复用：

```sql
CREATE DATABASE IF NOT EXISTS bookstore DEFAULT CHARACTER SET utf8mb4;
exit;
```

#### 第二步：启动原 Java 后端（新终端，保持运行）

```bash
cd /Users/zhuolifeng/work_dir/Web/backend
java -version
mvn -version
```

本模块的 [pom.xml](../backend/pom.xml) 编译目标为 Java 17，与 Task 4 的 Java 21 目标不同。使用 JDK 17 或兼容的更高版本；如果使用较新 JDK 遇到编译工具兼容问题，可在该终端切换已安装的 JDK 17 后重试。不要使用 Java 8。

数据库连接配置见 [application.properties](../backend/src/main/resources/application.properties)，默认连接本机 3306 的 `bookstore`。推荐通过当前终端环境变量覆盖真实账号密码，不必写入源码：

```bash
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD="$(python3 -c 'import getpass; print(getpass.getpass("MySQL password (hidden): "))')"
mvn spring-boot:run
```

输入第一步实际使用的 MySQL 密码；若账号不是 root，相应修改用户名。如果数据库地址不同，可在启动前额外设置 `SPRING_DATASOURCE_URL` 为实际 JDBC URL。

看到 Tomcat 在 8080 启动及 `Started BookstoreApplication` 后保持终端运行。首次启动会按现有配置执行 `schema.sql`、`data.sql` 建表并初始化样例数据；查看启动日志确认没有数据库认证或初始化错误。不要同时再启动第二个 8080 后端实例。

`mvn spring-boot:run` 在本地依赖缺失时会通过 Maven 下载依赖。若出现 `Access denied`，核对账号密码及 MySQL 授权；`Communications link failure` 通常需检查数据库是否运行、主机和端口是否正确。

#### 第三步：另开终端检查图书接口与实际 ISBN

```bash
curl -sS http://localhost:8080/api/v1/books | python3 -m json.tool
```

应返回 `code: 200`，其中 **`data` 为图书数组**，不是整个响应直接为数组。此查询接口允许匿名访问，无需 Task 4 的 JWT。

只打印 ISBN、书名和当前库存，便于选择实际存在的数据：

```bash
curl -sS http://localhost:8080/api/v1/books \
  | python3 -c 'import sys,json; body=json.load(sys.stdin); assert body["code"] == 200 and isinstance(body["data"], list), body; [print(b.get("isbn"), b.get("title"), "stock=", b.get("stock")) for b in body["data"]]'
```

若 `data` 为空，先检查数据库初始化日志或导入实际书籍数据，再进行库存演示。

#### 第四步：将 Task 2 助手切换到实时库存

在终端 B 按 Control+C，然后改为：

```bash
cd /Users/zhuolifeng/work_dir/Web
INVENTORY_MODE=live python3 scripts/run_assistant_live.py
```

按提示输入模型 API key。查询第三步中实际存在的 ISBN，并确认工具结果 `source` 为 `bookstore_live`，库存与 Java 接口当前值一致。可在网页发送，也可在另一个终端执行下面的 curl；将示例编号替换为实际数据，工具参数使用去掉连字符的 ISBN：

```bash
curl -sS http://127.0.0.1:8001/api/v1/assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"messages":[{"role":"user","content":"帮我查一下 ISBN为9787229030269 的图书当前库存。"}]}' \
  | python3 -m json.tool
```

`9787229030269` 对应仓库样例中的《三体（全集）》，仅在当前数据库确实有该 ISBN 时使用；实际库存以查询结果为准。此处只查库存，竞品报价仍属于模拟数据。

演示期间保持 MySQL、Java 后端和助手都运行。结束后在 Java 后端及助手各自的终端按 Control+C；若本次为演示专门启动了 MySQL，可按其原有管理方式停止。

Task 2 的基本 Function Calling 网页演示不需要切到这个模式。

### 3.7 使用 curl 展示 Function Calling

以下命令适用于 macOS 的 zsh/bash。只使用 curl 时不需要启动 React 前端，但必须先按 3.1 启动 8001 服务，并保持运行。模型 API key 已由服务进程读取，不需要放进 curl。

先检查服务状态：

```bash
curl -sS http://127.0.0.1:8001/health | python3 -m json.tool
```

正式演示应看到 `mode` 为 `live`、`modelConfigured` 为 `true`、`inventoryMode` 为 `demo`。

正常查询：

```bash
curl -sS http://127.0.0.1:8001/api/v1/assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"messages":[{"role":"user","content":"帮我查一下 ISBN为12345 的书还有多少本，顺便看看别家卖多少钱。"}]}' \
  | python3 -m json.tool  --no-ensure-ascii
```

截图展示 `data.answer` 和 `data.toolCalls`：正常情况下应包含 `check_inventory`、`get_competitor_price`，以及各自参数和结果。库存为演示数据 100 本，竞品为模拟报价 55.90 元。具体回答由真实模型生成。

错误 ISBN：

```bash
curl -sS http://127.0.0.1:8001/api/v1/assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"messages":[{"role":"user","content":"查一下 ISBN为abc 的库存和竞品价格。"}]}' \
  | python3 -m json.tool  --no-ensure-ascii
```

预期回答指出编号格式错误；模型可能直接询问正确 ISBN，因此不保证有工具调用记录。

模拟竞品超时：

```bash
curl -sS http://127.0.0.1:8001/api/v1/assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"messages":[{"role":"user","content":"查一下 ISBN为00000 的库存和竞品价格。"}]}' \
  | python3 -m json.tool  --no-ensure-ascii
```

展示库存 12 本及竞品工具的 `TIMEOUT`，模型应说明暂时无法取得报价。工具失败可以作为对话结果返回，不能把它等同于聊天接口必须返回 HTTP 500。

每条命令只发送一条用户消息，相当于网页里的“新对话”。如需连贯多轮对话，需在 `messages` 中按 user、assistant、user 的顺序传入历史。

## 4. Task 3：RAG + ReAct 导购网页演示

### 4.1 展示文档分块与向量化

另开“终端 C”，先执行：

```bash
cd /Users/zhuolifeng/work_dir/Web
python3 scripts/build_policy_index.py
```

当前政策文档应得到：

```text
8 chunks, 1045 dimensions, local-tfidf-char-unigram-bigram-l2-v1
```

生成的完整索引为 guide/data/policy_index.json。这里是本地 TF-IDF 词项向量，不是神经 Embedding API；如果政策文本改动，分块数量和词表维度可能变化。

### 4.2 启动真实模型导购

在终端 C 继续执行：

```bash
python3 scripts/run_guide.py
```

按提示输入 API key。默认使用学校 deepseek-chat，服务监听 8002，保持终端运行。

正式演示不要加 --demo；--demo 是不调用真实模型的规则替身。

### 4.3 打开网页

```bash
open http://localhost:3000/guide
```

确认显示“真实模型 ReAct Agent”。点击“查看政策分块与向量信息”，展示片段 ID、正文、维度和向量预览。

### 4.4 复杂案例：先查书，再查政策

点击“新对话”，输入：

```text
我想买一本关于微服务的书，另外如果我买了不喜欢，拆了塑封还能退吗？
```

展示页面上的执行顺序：

```text
Thought：简短行动理由
Action：search_book_catalog
Observation：模拟微服务书目

Thought：需要进一步确认政策
Action：query_store_policy
Observation：RAG 返回的相关政策片段和相似度

Thought：整理有依据的回答
Action：finish
Observation：最终回答
```

具体 Thought 措辞和参数由模型生成，不要求与示意文本逐字一致。展开 Observation，核对实际结果；展开政策来源，检查回答中的 [P02-01]、[P01-01] 等引用确实来自检索片段。

### 4.5 只问退货：展示 AI 的行动理由与政策检索

点击“新对话”，输入：

```text
我想退货，拆了塑封之后发现不喜欢内容，还能退吗？
```

预期先调用 query_store_policy，再 finish；不需要调用查书工具。

应说明课程模拟政策中的“拆封后仅因不喜欢不支持无理由退货”，并区分缺页等质量问题。展示 Thought、Action、Observation 以及政策引用。

Thought 是公开的简短行动理由，不是模型内部隐藏思维链。实际向量化由 RAG 工具执行，不能仅凭模型一句“我进行了向量化”证明计算已经发生。

### 4.6 在终端额外展示问题向量与检索分数

如果评阅者需要看实际向量，在一个空闲终端运行以下整段代码；它不需要模型 API：

```bash
cd /Users/zhuolifeng/work_dir/Web
python3 - <<'PY'
from guide.agent import policy_index, query_store_policy

question = '我想退货，拆了塑封之后发现不喜欢内容，还能退吗？'
index, metadata = policy_index()
vector = index.embed(question)
print('问题：', question)
print('算法：', metadata['embedding'])
print('政策片段数量：', len(index.chunks))
print('共享词表大小：', len(index.vocabulary))
print('问题向量维度：', len(vector))
print('部分非零分量：')
shown = 0
for position, value in enumerate(vector):
    if value:
        print(position, index.vocabulary[position], round(value, 6))
        shown += 1
        if shown == 10:
            break
print('\n实际 RAG 检索结果：')
for hit in query_store_policy(question)['matches']:
    print(hit['id'], 'score=', hit['score'])
    print(hit['text'])
PY
```

这一步证明问题使用与政策相同的词表和 IDF 转换为向量，再计算相似度。网页中的行动记录则展示模型何时决定调用该工具。

### 4.7 需要单独保存本次退货案例日志时

在空闲终端执行：

```bash
cd /Users/zhuolifeng/work_dir/Web
python3 scripts/run_guide_case.py \
  --live \
  --question '我想退货，拆了塑封之后发现不喜欢内容，还能退吗？' \
  --output guide/evidence/live-return-case.json
```

该命令是另外执行一次真实模型案例，不是导出浏览器已有对话；它会生成自己的 JSON 和 .log，完成后自动退出。网页演示本身已有终端 C 的执行日志。

### 4.8 使用 curl 展示 RAG 与 ReAct

先按 4.1、4.2 构建索引并启动 8002 服务。curl 演示不需要 React 前端。

检查真实模型模式和索引信息：

```bash
curl -sS http://127.0.0.1:8002/health | python3 -m json.tool  --no-ensure-ascii
```

应看到 `mode` 为 `live`、`modelConfigured` 为 `true`，以及 `embedding`、`chunkCount`、`dimensions`。

查看政策分块和向量预览（不调用大模型）：

```bash
curl -sS http://127.0.0.1:8002/api/v1/guide/rag | python3 -m json.tool  --no-ensure-ascii
```

展示 `chunks` 中的片段 ID、正文和 `vectorPreview`。向量预览只包含各向量前 12 个分量，并非完整向量；某些片段的这些分量可能全为零。问题向量及实际检索分数可按 4.6 展示。

复杂案例，展示先查书再查政策：

```bash
curl -sS http://127.0.0.1:8002/api/v1/guide/chat \
  -H 'Content-Type: application/json' \
  -d '{"messages":[{"role":"user","content":"我想买一本关于微服务的书，另外如果我买了不喜欢，拆了塑封还能退吗？"}]}' \
  | python3 -m json.tool  --no-ensure-ascii
```

截图展示 `data.trace` 中每步的 `thought`、`action`、`observation`，预期包含 `search_book_catalog`、`query_store_policy` 和 `finish`；再展示 `data.answer` 与 `data.sources`，核对政策引用。具体步骤以模型实际返回为准，`thought` 是公开行动理由。

只问退货政策：

```bash
curl -sS http://127.0.0.1:8002/api/v1/guide/chat \
  -H 'Content-Type: application/json' \
  -d '{"messages":[{"role":"user","content":"我想退货，拆了塑封之后发现不喜欢内容，还能退吗？"}]}' \
  | python3 -m json.tool  --no-ensure-ascii 
```

预期调用 `query_store_policy` 后给出带政策片段引用的回答。以上 POST 请求均会调用真实模型；等待当前命令结束再演示下一条，并遵守学校 API 限流。

## 5. Task 4：JWT + 虚拟线程的网页报告演示

### 5.1 启动独立后端

另开“终端 D”：

```bash
cd /Users/zhuolifeng/work_dir/Web
python3 cloud-backend/scripts/start.py
```

本机现有 Java 24 可以使用；源码编译目标为 Java 21。服务默认监听 8081，保持运行。

此任务不用输入模型 API key。未配置 JWT_SECRET 时，启动脚本会在进程内生成临时签名密钥；重启服务后需要重新获取 Token，验收脚本会自动处理。

### 5.2 执行实际请求并生成报告

另开“终端 E”：

```bash
cd /Users/zhuolifeng/work_dir/Web
python3 cloud-backend/scripts/verify_api.py
```

该脚本自动使用教学账号登录，JWT 只放在内存，不需要手工复制 Token。预期七项检查全部 PASS，包含：

- 无 Token：401。
- 有效 JWT：200，并且虚拟线程标志为 true。
- 错误 Token：401。
- 仅有 Session Cookie：401。
- 成功请求后不带 Token：仍为 401。
- 非法 ID：400；不存在的图书：404。

### 5.3 浏览器打开报告

继续在终端 E 执行：

```bash
open cloud-backend/evidence/auth-report.html
```

展示报告前两张卡片：

```text
未携带 Token → HTTP 401
有效 JWT → HTTP 200 + 图书
X-Request-Thread-Virtual → true
X-Java-Version → 当前实际 Java 版本
```

本页由刚才的实际 HTTP 响应生成，不是预设结果，也不是 React 页面；修改或重启服务后，应重新运行 verify_api.py 才能更新报告，单独刷新旧 HTML 不会重新发送 API 请求。

直接在浏览器地址栏打开 http://127.0.0.1:8081/api/books/1 会因没有 Authorization 头返回 401，这是正常行为。有效 JWT 的 200 由验收脚本携带请求头取得，并展示到报告中。

### 5.4 保存截图并更新提交包

在报告页按 Shift+Command+4，框选 401 与 200 两张卡片，保存为：

```text
cloud-backend/evidence/auth-401-200.png
```

然后执行：

```bash
cd /Users/zhuolifeng/work_dir/Web
python3 cloud-backend/scripts/package_submission.py --require-screenshot
```

出现 Screenshot: included，说明 PNG 已包含。最终上传 submission/ebookstore-java21-jwt-submission.zip。

### 5.5 使用 curl 展示 401、200 和虚拟线程

先按 5.1 启动 8081 服务。在另一个 macOS zsh/bash 终端依次执行；以下命令中的 `-i` 会同时显示实际 HTTP 状态、响应头和响应体。

不带 Token，应返回 HTTP 401：

```bash
curl -i http://127.0.0.1:8081/api/books/1
```

登录并获取 JWT，保存在当前终端变量中（演示账号若通过环境变量改过，需相应修改）：

```bash
TOKEN=$(curl -sS http://127.0.0.1:8081/api/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"demo-password"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])')
```

这一步通过 `POST /api/auth/token` 实际签发 JWT；正常情况下不会打印 Token。登录失败时提取会报错，应先检查服务及演示账号。

携带有效 Token，应返回 HTTP 200 和图书数据：

```bash
curl -i http://127.0.0.1:8081/api/books/1 \
  -H "Authorization: Bearer $TOKEN"
```

核对成功响应头（大小写可能不同）：

```text
X-Request-Thread-Virtual: true
X-Request-Thread-Name: tomcat-handler-...
X-Java-Version: 当前实际版本
```

`true` 来自实际请求线程的 `Thread.currentThread().isVirtual()`。项目没有 `/api/thread-info` 接口；使用这些响应头展示线程信息。源码目标是 Java 21，运行版本应按响应头如实展示。

错误 Token，应返回 HTTP 401：

```bash
curl -i http://127.0.0.1:8081/api/books/1 \
  -H 'Authorization: Bearer invalid-token'
```

成功请求后再次不带 Token，应仍返回 HTTP 401：

```bash
curl -i http://127.0.0.1:8081/api/books/1
```

仅携带 Session Cookie，应返回 HTTP 401：

```bash
curl -i http://127.0.0.1:8081/api/books/1 \
  -H 'Cookie: JSESSIONID=not-authenticated'
```

截图时优先框选“不带 Token 的 401”和“有效 Token 的 200”，保留请求命令、状态行、图书数据及虚拟线程响应头。可以将此终端截图保存为 5.4 的 `cloud-backend/evidence/auth-401-200.png`，再按 5.4 更新提交包。不要在截图中展开真实 JWT。

若只做终端演示，不需要运行 React 或生成 HTML。`curl -i` 用于展示原始 HTTP 证据；Task 2/3 的 `python3 -m json.tool` 用于格式化 JSON，不会显示 HTTP 状态行。需要查看 Task 2/3 的状态与响应头时，可移除 JSON 格式化管道，并将 `curl -sS` 改成 `curl -i`。

## 6. 演示顺序建议

1. 终端 A 启动 React，只启动一次。
2. Task 2：启动终端 B，网页演示正常 ISBN、错误 ISBN、模拟超时。
3. Task 3：终端 C 构建索引并启动服务，网页演示复杂案例和退货问题，展开来源与执行记录。
4. Task 4：终端 D 启动后端，终端 E 验收后打开 HTML，展示 401、200、虚拟线程并截图。
5. 演示结束后，在 A/B/C/D 各按 Control+C。终端 E 中的验收命令本身会自动结束。

Task 2/3 每分钟合计模型请求应注意学校 API 的 10 次请求限制；一个问题通常需要多次模型请求。分开演示，遇到限流等待后重试，不要同时连续发送多条问题。

## 7. 常见问题与需要自行执行的安装命令

### 已有服务占用端口

先查看对应端口：

```bash
lsof -nP -iTCP:3000 -sTCP:LISTEN
lsof -nP -iTCP:8001 -sTCP:LISTEN
lsof -nP -iTCP:8002 -sTCP:LISTEN
lsof -nP -iTCP:8081 -sTCP:LISTEN
```

如果是已经启动的本任务服务，可直接复用；需要重启时，在原终端按 Control+C 后再启动。

### 前端尚未安装依赖

你当前已经安装；只有缺失时才由你自行执行：

```bash
cd /Users/zhuolifeng/work_dir/Web/frontend
npm ci
```

### Task 4 提示 Maven 离线依赖缺失

由你自行执行一次下载与构建：

```bash
cd /Users/zhuolifeng/work_dir/Web/cloud-backend
mvn -DskipTests package
```

随后回到项目根目录，用 start.py 启动。Task 2/3 的 Python 代码只用标准库，无需 pip 安装。

如果 Maven 错用了 Java 17，而本机已安装 Homebrew Java 24，可先在同一终端设置：

```bash
export JAVA_HOME="$(brew --prefix openjdk)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
```

### 修改政策或修复 Python 文件后

重新生成索引，并在终端 C 停止后重启导购服务，避免继续使用旧进程中的代码或缓存：

```bash
cd /Users/zhuolifeng/work_dir/Web
python3 scripts/build_policy_index.py
python3 scripts/run_guide.py
```

第二条命令前应确保旧导购进程已经停止。

## 8. 相关文档

- [Task 2 实现说明](../assistant/TASK2_IMPLEMENTATION.md)
- [Task 3 实现说明](../guide/TASK3_IMPLEMENTATION.md)
- [Task 4 实现说明](../cloud-backend/IMPLEMENTATION.md)
- [Task 1–4 总结与联系](TASKS_1_TO_4_OVERVIEW.md)
