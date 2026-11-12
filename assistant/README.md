# 电子书店客服 AI 助手：Function Calling 作业

实现过程与评分项说明见 [Task 2 实现说明](TASK2_IMPLEMENTATION.md)。

本次新增一个 Python 助手服务和 React 对话页面。Python 只依赖标准库（Python 3.10+），无需 pip 安装。前端复用原工程的 React / Ant Design，无新增 npm 依赖。安装命令由使用者自行执行。

## 1. 作业实现与评分对应

| 评分项 | 实现位置与演示方式 |
| --- | --- |
| A：Function Schema | `assistant/server.py` 中的 `TOOLS`，包含两个函数名、中文描述、object 参数、string 类型的必填 isbn、禁止额外字段、strict 标记 |
| B：解析 Tool Call | `OpenAIModel.complete()` 接收 Chat Completions 返回；`run_chat()` 解析 `tool_calls`、函数名、arguments 与调用 ID；`execute_tool()` 解析参数 JSON 并校验 |
| C：本地调用 | `check_inventory(isbn)` 实时读取现有书店后端；`get_competitor_price(isbn)` 发起实际 HTTP 请求，访问本机模拟竞品 API；可显式开启演示库存 |
| D：结果回传与回答 | 保存 assistant 的 tool_calls，为每次调用追加 role=tool、tool_call_id 和结果，再请求模型，直至收到最终自然语言回答；React 页面显示回答 |
| E：异常与纠正 | ISBN 错误、未知工具、JSON 错误、接口断连、模拟 HTTP 504、重试上限、模型响应异常均处理。错误以结构化工具结果反馈模型，允许模型纠正参数、询问用户或重试一次 |

前端地址为 `http://localhost:3000/assistant`，左侧菜单新增“智能助手”。页面展示运行模式、自然语言回复及可展开的工具调用记录；Python 终端和浏览器 Console 同时打印函数名、参数和结果。

## 2. 启动：真实模型 + 实时库存

先按原工程说明启动 MySQL 和 Spring Boot 后端，确认 `http://localhost:8080/api/v1/books` 返回当前仓库格式的图书列表，并有需要查询的 ISBN 与库存。既有启动方式不变。本次没有安装或打包 JAR，也没有修改 Java 后端。

在工程根目录运行：

```bash
python3 scripts/run_assistant_live.py
```

脚本默认使用学校提供的公开 API 地址 `https://models.sjtu.edu.cn/api/v1` 和调用名 `deepseek-chat`，启动时提示输入密钥，输入不回显，也不写入磁盘。密钥只存在于当前进程环境。停止服务使用 Ctrl+C。

另开终端，在原前端目录运行：

```bash
cd frontend
# 若尚未安装依赖，由你自行执行：npm ci
npm start
```

打开 `http://localhost:3000/assistant`。输入数据库中真实存在的 ISBN；工具每次重新读取后端，不缓存库存。修改同一本书的库存后再次询问，可验证数据刷新。查询不存在的 ISBN 返回 NOT_FOUND，不能当作库存为零。

如果学校 API 使用其他模型，可在启动前设置 `OPENAI_MODEL=deepseek-reasoner`、`qwen` 或 `qwen3.8-27b`；本次实际联调使用 `deepseek-chat`，没有宣称其他模型已验证。适配器会在模型返回时保留 reasoning_content，以便兼容需要回放推理字段的服务。

## 3. 启动：真实模型 + 演示库存（推荐课堂录像）

无需启动 MySQL / Java 后端：

```bash
INVENTORY_MODE=demo python3 scripts/run_assistant_live.py
```

仍然是真实大模型 API 调用，仅库存使用演示夹具；页面、工具结果和模型提示均明确其为演示数据。启动前确保原来占用 8001 端口的助手服务已停止。

示例问题：

1. `帮我查一下 ISBN为12345 的书还有多少本，顺便看看别家卖多少钱`
   - 两个工具均应调用；演示库存 100 本，模拟竞品 55.90 元。
2. `查一下 ISBN为abc 的库存和竞品价格`
   - 若工具被调用则返回 INVALID_ISBN，模型应要求正确 ISBN，不编造答案。模型也可以在调用前识别明显错误并直接要求更正，不强迫对错误参数发起查询。
3. `查一下 ISBN为00000 的库存和竞品价格`
   - 演示库存 12 本；模拟竞品 API 返回 HTTP 504；可重试一次，仍失败后说明无法查询。
4. `查一下 ISBN为978-7536692930 的库存和竞品价格`
   - 模型可依据明确的原文去除连字符再调用；不得猜测或替换为其他 ISBN。竞品模拟价 55.90 元；库存夹具无此书，应说明未找到。

实时库存查询会在比较时去除数据库 ISBN 中的连字符与空白，兼容现有样例数据，不修改原始 ISBN。ISBN 作为字符串保存，以保留前导零；接受 5 位课堂演示编号及 10/13 位 ISBN 格式，不执行校验位运算，不引入 ISBN 唯一约束。同一 ISBN 匹配多条记录时返回各条库存及合计数量，供模型说明。

## 4. 完全离线演示

```bash
sh scripts/run_assistant_demo.sh
```

此模式不调用真实模型，使用确定性规则模拟同样的工具协议，适合无网络检查界面。必须区分：离线模式不能作为“模型成功触发 Function Calling”的评分证据。正式演示请使用第 2 或第 3 节的真实模型模式。

## 5. 参数与运行说明

| 环境变量 | 默认值 / 作用 |
| --- | --- |
| `OPENAI_API_KEY` | 仅服务端读取；不得放入前端或提交包 |
| `OPENAI_BASE_URL` | 启动脚本默认学校 API；直接运行 server.py 默认官方 OpenAI API |
| `OPENAI_MODEL` | 启动脚本默认 deepseek-chat；直接运行 server.py 默认 gpt-4o-mini |
| `ASSISTANT_MODE` | live / demo，默认 live；live 脚本强制 live |
| `INVENTORY_MODE` | live / demo，默认 live，不会在后端失败时静默降级 |
| `BOOKSTORE_API_BASE` | http://127.0.0.1:8080/api/v1 |
| `ASSISTANT_PORT` | 8001，仅监听本机回环地址 |
| `COMPETITOR_API_URL` | 默认本助手服务的 /mock/competitor-price，数据始终标注模拟 |
| `FRONTEND_ORIGINS` | 默认允许 localhost:3000 和 127.0.0.1:3000 |
| `REACT_APP_ASSISTANT_URL` | 前端构建/启动时读取，默认 http://localhost:8001 |

本次使用 Chat Completions 兼容协议，向模型发送 `tools` 和 `tool_choice=auto`。每次模型请求最多输出 1200 tokens，每个用户问题最多进行 5 轮模型交互，以限制错误循环。学校 API 限制每分钟 10 次请求；一个问题通常产生 2–3 次模型请求，避免连续快速点击或并发演示，遇到限流稍后重试。周配额和共享账户其他请求仍由提供方统计。

`00000` 通过 HTTP 504 模拟竞品超时；客户端还实际配置 3 秒竞品超时、8 秒库存请求超时和 30 秒模型请求超时。错误作为工具反馈回传；服务端对相同参数的两次超时后阻止继续执行，且有总轮数上限。工具参数只经白名单派发，不使用 eval。

本实现针对当前仓库现有 GET /api/v1/books 的数组响应读取数据，没有把之前 OpenAPI 目标分页契约当作已实现接口。原有六项图书契约未改动，助手接口由独立的 8001 端口提供。

官方协议参考：https://developers.openai.com/api/docs/guides/function-calling

## 6. 测试与证据

所有依赖由你自行安装。安装完成后在工程根目录运行：

```bash
sh scripts/test_assistant.sh
```

单独运行 Python 测试：

```bash
python3 -m unittest discover -s tests/assistant -v
```

Python 测试使用本机临时 HTTP 服务，不访问收费大模型。覆盖多工具调用、tool_call_id 对应、错误反馈纠正、模拟 504、实时重新获取库存、重复 ISBN、无密钥、异常响应及循环上限。

启动真实模型服务后，可以生成新的真实调用证据：

```bash
python3 scripts/smoke_assistant.py --scenario normal --output assistant/live-normal.json
python3 scripts/smoke_assistant.py --scenario invalid --output assistant/live-invalid.json
python3 scripts/smoke_assistant.py --scenario timeout --output assistant/live-timeout.json
```

注意每分钟 10 次模型请求限制；分开运行并等待，勿反复快速执行全部场景。证据仅包含问题、工具调用和回答，不含密钥。最终实际执行结果见 `assistant/VALIDATION.md`。

## 7. 如何提交

提交包是本次编写/修改的文件集合，不是整个工程。需要在原 E-BookStore 工程根目录解压覆盖使用（保留已有工程的其他文件）。

1. 本地先按第 3 节运行真实模型演示，确认正常、错误 ISBN、超时三个场景；展开工具调用记录，展示函数名、参数、返回结果和最终回答。
2. 如老师要求截图或录像，可在作业平台另外上传这些展示材料。不要拍到 API 密钥输入、环境变量或任何密钥内容。
3. 在根目录运行 `python3 scripts/package_assistant_submission.py` 重新生成提交包。
4. 上传 `submission/ebookstore-assistant-submission.zip` 到课程作业平台。可在文件名中补上学号姓名；无需上传整个 Web 工程。
5. 压缩包内含源码、前端集成文件、测试、运行脚本、说明、真实调用证据及文件清单。明确不包含 node_modules、build、target、JAR、.env、.git 或真实密钥。
6. 这份作业需要在原工程中运行，README 已说明集成位置与启动方法。API 密钥由评阅者在启动时自行提供，不随包提交。

我无法代你登录未知的课程平台上传；最终上传动作由你完成。
