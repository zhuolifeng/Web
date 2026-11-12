# 实际校验记录与范围

本文件记录本次开发期间实际执行的检查，不将测试替身、演示库存或目标设计声称为真实数据库验收。

## 已执行结果

- Python：`python3 -m unittest discover -s tests/assistant -v`，最后一次执行为 22 项全部通过。覆盖函数 Schema、多工具解析与回传、ISBN 参数纠正、HTTP 504、重试上限、数据库 ISBN 连字符匹配、库存重新读取、重复 ISBN、未知工具和无密钥处理。
- React：`CI=true npm test -- --watchAll=false --runInBand --runTestsByPath src/__tests__/AssistantPage.test.js`，最后一次执行为 7 项全部通过。覆盖回答和调用记录展示、模型配置错误、中文输入法，以及 StrictMode 健康检查取消误报和恢复连接后的状态更新。
- JSX 语法：App.js、Layout.js、AssistantPage.js 和 AssistantPage.test.js 曾通过本地 Babel parser 检查。
- 生产构建：开发期间 `npm run build` 曾成功。其后修复了连接状态处理及 ISBN 连字符匹配；修复后的相关测试已通过，未再次执行生产构建。接收者可按 README 自行复检。
- `git diff --check` 在最后一次代码修复后通过。

## 真实大模型 API 联调

使用学校提供的兼容 API 和 deepseek-chat，已完成真实 HTTP 请求。密钥只放在运行进程中，没有写入源码或证据。

| 场景 | 模型模式 | 库存来源 | 实际工具调用次数 | 结果证据 |
| --- | --- | --- | --- | --- |
| 正常库存与竞品价格 | live | demo | 2 | live-normal.json |
| 明显错误 ISBN | live | demo | 0 | live-invalid.json |
| 竞品超时与重试 | live | demo | 3 | live-timeout.json |

正常场景：真实模型触发两个函数，接收工具结果后回答演示库存 100 本、模拟竞品 55.90 元。

错误 ISBN 场景：真实模型在调用工具前直接识别 abc 格式错误并询问正确 ISBN，因此没有 Tool Call。这是实际记录，不将其描述为模型已接收到 INVALID_ISBN 工具错误。工具错误后再纠正参数的完整协议闭环由 Python 的 CorrectingModel 测试替身验证。

超时场景：库存工具成功返回演示库存 12 本；竞品 HTTP API 两次返回模拟 504；真实模型在重试一次后说明报价暂不可用。

## 尚需本机人工验收

- 当前提交附带的真实模型联调证据使用演示库存，不是 MySQL 库存。
- 真实库存适配器通过本机 HTTP 测试替身验证；使用者仍需按 README 启动自己的 MySQL / Java 后端，查询实际 ISBN，并修改库存后再次查询。
- 页面交互通过 React 自动测试；自动浏览器视觉检查因本机权限不可用而未完成。使用者提供的实际页面截图与请求日志展示过成功调用；截图中连接误报的代码随后已修复并通过回归测试。
- 其他模型调用名未进行真实联调。

## 提交包校验

打包脚本采用固定文件白名单，生成 SHA-256 文件清单，并检查 ZIP 完整性、排除目录与 API key 常见格式。不会压缩原工程、node_modules、target、build、JAR、.env、.git 或 Python 缓存。
