# 实际校验记录

日期：2026-10-24。

## 环境与版本

- 实际运行：Java 24.0.1，按用户明确要求使用现有版本。
- 编译目标：Java 21；生成的 CloudApplication.class 主版本为 65。
- Spring Boot：3.2.5，嵌入式 Tomcat。
- Maven：3.9.10；JJWT：0.12.5。
- 没有声称在 JDK 21 上执行过本次测试。Java 21 基线的虚拟线程 API 已在 Java 24 的实际请求中生效。

## 已执行检查

1. `mvn -o -f cloud-backend/pom.xml test`：23 项测试，0 失败、0 错误、0 跳过，BUILD SUCCESS。使用真实嵌入式 Tomcat 和 HTTP 客户端；包含 40 个并发请求与平台线程对照。脱敏 Surefire 摘要在 evidence/test-summary.json。
2. `verify_api.py --url http://127.0.0.1:18081`：7 项 HTTP 检查全部通过；401、200、400、404 符合预期。所有检查都记录到真实 Java 24 版本；成功请求虚拟标志为 true，无 Set-Cookie。证据为 auth-results.json、auth-report.html。
3. `benchmark.py`：对 18081 虚拟线程服务和 18082 平台线程服务各执行 100 请求、50 并发；均 100 次成功。测得 191.5 与 90.16 请求/秒，实际请求标志分别 true / false，原始结果在 benchmark-results.json。
4. Python 脚本语法编译通过；Java 源码通过 Maven 编译，编译输出主版本 65。
5. 测试期间只运行离线 Maven 命令，没有安装 JDK、下载或安装新的 Maven/Python 依赖。

首次沙箱内 Tomcat 测试因监听端口受限而失败，随后在允许本机端口的环境中实际重新执行并通过；没有把首次失败直接标记为通过。

## 证据边界与待完成项

- 图书来自课堂模拟的不可变目录，Thread.sleep 模拟慢数据库 I/O，不是实际 MySQL 性能测试。
- 性能结果是固定客户端并发和 20 平台工作线程控制下的本机实验，不能外推为任意上云环境的提升比例。
- 用户已提供实际验收页面截图，展示 http://127.0.0.1:8081 的无 Token 401、有效 JWT 200、错误 Token 401、Cookie 401，以及 Java 24.0.1 与虚拟线程标志 true。截图已原样保存为 evidence/auth-401-200.png。
- 最终提交包已通过 --require-screenshot 检查并包含该 PNG。截图由用户提供，不是代理绕过电脑 UI 权限拍摄，也不是生成图示。
- 所有本次启动的临时 18081 / 18082 服务已停止，确认端口无监听；未停止用户自己的其他服务。

## 敏感信息

验收脚本不保存有效 JWT、登录密码或签名密钥。登录 Token 只在内存中使用，截图报告 Authorization 使用 REDACTED。测试源码中的固定签名串仅用于测试夹具，不用于启动服务。
