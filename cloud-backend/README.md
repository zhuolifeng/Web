# Spring Boot 虚拟线程与 JWT 无状态 API

独立的 `cloud-backend` 模块，默认使用 8081 端口，不需要 MySQL、Python 助手或前端。原书店工程、Task 2/3 的接口保持不变。

源码编译目标为 Java 21，已按用户确认在 Java 24.0.1 上完成实际运行。Java 24 具备虚拟线程，不需要为本次运行卸载或替换现有 JDK。没有声称本次测试运行在 JDK 21 上。

## 1. 安装与环境准备：由你自己执行

你当前已经有 Java 24、Maven 和 Python 3，没有必须新增安装的软件。

检查：

```bash
java -version
mvn -version
```

运行环境需 Java 21 或以上。若 Maven 使用的是 Java 17，可在本机终端选择已安装的 Java 24：

```bash
export JAVA_HOME="$(brew --prefix openjdk)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
```

如果其他电脑缺少工具，由你自行安装，例如：

```bash
brew install openjdk@21 maven
```

启动与测试脚本默认用 Maven 的 `-o` 离线模式，不会代你下载依赖。若出现缺少 Maven artifact 或插件的提示，请你自行执行一次：

```bash
cd /Users/zhuolifeng/work_dir/Web/cloud-backend
mvn -DskipTests package
```

这会由 Maven 下载依赖并在本地生成 target，不需要把其中任何 JAR 提交。执行完成后再启动。

## 2. 启动服务

从原工程根目录执行：

```bash
cd /Users/zhuolifeng/work_dir/Web
python3 cloud-backend/scripts/start.py
```

脚本会选取现有 Java 21+，本机可使用已安装的 Java 24；没有符合条件的 JDK 才会提示你自行安装。`--allow-java24` 只是兼容旧调用的可选参数，Java 24 默认就被接受。

服务默认监听 `http://127.0.0.1:8081`。启动后保持终端运行，Ctrl+C 停止。

未设置 JWT_SECRET 时，脚本仅在进程环境中生成随机签名密钥，不打印或保存。服务重启后旧 Token 失效，需要重新登录。若未来多实例共享 Token，应由环境配置相同且稳定的密钥；源码不保存真实密钥。

课堂模拟账号：

```text
username: student
password: demo-password
```

这是本独立模块的公开演示账号，不是原书店数据库中的账号。可用 DEMO_USERNAME / DEMO_PASSWORD 环境变量覆盖。

## 3. 一条命令检查 401、200 和虚拟线程

服务启动后，在另一个终端执行：

```bash
cd /Users/zhuolifeng/work_dir/Web
python3 cloud-backend/scripts/verify_api.py
```

脚本在内存中登录获取 JWT，依次发起实际 HTTP 请求，检查：

- 无 Token → 401。
- 有效 Token → 200，且 X-Request-Thread-Virtual 为 true。
- 错误 Token → 401。
- 只有 Session Cookie → 401。
- 成功请求后下一次不带 Token → 仍为 401。
- 非法 ID → 400；不存在的图书 → 404。

还会检查没有 Set-Cookie，并记录真实 Java 版本。报告不会保存 JWT 或登录密码：

```text
cloud-backend/evidence/auth-results.json
cloud-backend/evidence/auth-report.html
```

默认核对 Java 主版本 >=21，Java 24 正常通过。报告按响应头记录实际版本，不把 Java 24 伪装成 21。

## 4. 必须由你保存测试截图

评分项要求截图。提交包已包含用户实际提供的 `evidence/auth-401-200.png`，展示无 Token 的 401、有效 JWT 的 200 和虚拟线程响应头。HTML / JSON 是对应的实际请求报告，不能代替截图。以下步骤用于重新验收时更新截图。

执行验收脚本后，在 macOS 打开：

```bash
open cloud-backend/evidence/auth-report.html
```

报告前两张卡片并列显示“无 Token：401”和“有效 JWT：200”，成功卡片还展示实际请求线程虚拟标志及 Java 版本。

按 Shift+Command+4，框选前两张卡片截图，将图片保存或移动为：

```text
cloud-backend/evidence/auth-401-200.png
```

报告里的 Authorization 已隐藏，无需拍摄真实 Token。页面由真实 HTTP 结果生成；不要将生成的 HTML 文件本身称为浏览器 Network 截图。

保存图片后重新打包：

```bash
python3 cloud-backend/scripts/package_submission.py --require-screenshot
```

该选项会要求真实图片存在后才生成最终包，避免漏交截图评分项。

## 5. 手工 curl 检查

不带 Token：

```bash
curl -i http://127.0.0.1:8081/api/books/1
```

预期 HTTP 401，响应为 code=401、data=null。

登录获取 Token：

```bash
curl -sS -X POST http://127.0.0.1:8081/api/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"demo-password"}'
```

成功结果中的 `data.accessToken` 是 JWT。若想避免复制和展示 Token，优先使用第 3 节的自动脚本。手工请求时在下列占位符中填入本次生成的 Token：

```bash
curl -i http://127.0.0.1:8081/api/books/1 \
  -H 'Authorization: Bearer <本次生成的JWT>'
```

预期 HTTP 200，响应 data 为图书；响应头包含：

```text
X-Request-Thread-Virtual: true
X-Request-Thread-Name: tomcat-handler-...
X-Java-Version: 实际运行版本
```

实际请求线程使用 `Thread.currentThread().isVirtual()` 检测，不是另开一个虚拟任务后伪装成 Web 请求线程。

## 6. 自动测试

```bash
cd /Users/zhuolifeng/work_dir/Web
sh cloud-backend/scripts/test.sh
```

脚本不安装依赖。JUnit 真实启动随机端口的嵌入式 Tomcat，不是 MockMvc，也没有 mock 请求执行线程。覆盖 23 项测试，包括 JWT 生成、过期、篡改、发行方、必要声明、无状态行为、400/404、40 个并发请求，以及关闭虚拟线程后的平台线程对照。

结束后只导出不含密钥和 JWT 的 Surefire 摘要到 `evidence/test-summary.json`，不打包可能包含环境信息的完整测试 XML。

## 7. 可选：自行复现并发 I/O 对照

普通服务在 8081 使用虚拟线程，默认每次查询 sleep 200ms。另开终端启动平台线程对照：

```bash
python3 cloud-backend/scripts/start.py --platform --port 8082
```

保持两个服务运行，随后执行：

```bash
python3 cloud-backend/scripts/benchmark.py \
  --url http://127.0.0.1:8081 \
  --platform-url http://127.0.0.1:8082 \
  --requests 100 --concurrency 50
```

对照组限制为 20 个平台工作线程，虚拟线程组不使用这个工作线程池上限。两组均做 5 次预热，再各发送 100 个请求。输出实际成功数、总耗时、吞吐、平均与 P95 延迟、线程标志和 Java 版本。

这是固定参数下的本机合成实验。虚拟线程适合大量阻塞等待，不意味着单次 200ms I/O 变快，也不保证任意生产负载都有相同比例提升。客户端线程、CPU、网络、数据库连接池等都可能影响结果。

本次实测见 `evidence/benchmark-results.json` 与 `VALIDATION.md`。测试完在两个终端各按 Ctrl+C。

## 8. 接口与配置

| 接口 | 认证要求 | 结果 |
| --- | --- | --- |
| POST /api/auth/token | 演示用户名和密码 | 生成 JWT，Cache-Control: no-store |
| GET /api/books/{id} | Authorization: Bearer JWT | 200 图书；无效认证 401；非法 ID 400；不存在 404 |

图书为不可变的课堂模拟数据，价格保持字符串，无数据库写操作。无状态认证不等于所有业务数据都不能存储；这里不保存客户端 Session 或登录状态。

| 环境变量 | 默认 / 作用 |
| --- | --- |
| SERVER_PORT | 8081；启动脚本通过 --port 指定 |
| JWT_SECRET | 启动脚本生成临时值，或由你自行配置 >=32 字节文本 |
| JWT_TTL_SECONDS | 3600，允许 1–86400 |
| DEMO_USERNAME / DEMO_PASSWORD | student / demo-password |
| BOOK_IO_DELAY_MS | 200，允许 0–5000 |
| PLATFORM_THREAD_LIMIT | 20，仅平台线程对照使用 |
| VIRTUAL_THREADS_ENABLED | true；启动脚本 --platform 时设置 false |

## 9. 提交内容

Task 4 实现思路见 [IMPLEMENTATION.md](IMPLEMENTATION.md)，实际结果见 [VALIDATION.md](VALIDATION.md)。四项任务的总结与关系见 [Task 1–4 总结](../docs/TASKS_1_TO_4_OVERVIEW.md)。

提交包只包含自编 Java 源码、pom、配置、脚本、文档、测试与证据，排除 target、第三方 JAR、整个 Web 工程与真实密钥。项目需要的第三方库由评阅者通过 Maven 获取，不随包上传。

先生成源码包：

```bash
python3 cloud-backend/scripts/package_submission.py
```

完成截图后，用 `--require-screenshot` 重新打包。最终上传：

```text
submission/ebookstore-java21-jwt-submission.zip
```

文件名中的 java21 表示源码目标与功能基线；本次实际测试使用用户确认的 Java 24，证据中保留真实版本。
