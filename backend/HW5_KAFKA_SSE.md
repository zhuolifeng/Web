# 作业 5：Kafka 异步下单 + SSE 实时推送

## 1. 需求与实现对照

| 要求 | 实现 |
| --- | --- |
| 本地启动单节点 Kafka | `kafka/docker-compose.yml`（KRaft 单节点，`localhost:9092`） |
| `POST /api/orders` 投递订单 JSON 到 `order-events`，返回 202 + 排队 ID | `controller/AsyncOrderController#submit` → `messaging/OrderEventProducer` |
| `@KafkaListener` 异步消费，模拟扣减库存耗时 | `messaging/OrderEventConsumer`（`Thread.sleep` 模拟耗时，随后真实扣减库存并生成订单） |
| `GET /api/orders/{id}/stream` 基于 `SseEmitter` 推送结果 | `AsyncOrderController#stream` → `messaging/OrderSseHub` |
| 消费结果与 SSE 打通 | 消费者处理完调用 `OrderSseHub.publish()`，推送给该排队单的所有订阅者 |

## 2. 流程

```
浏览器/客户端                 Spring Boot                                Kafka
     │ POST /api/orders           │                                       │
     │───────────────────────────▶│ 快照购物车 → OrderEvent(JSON)           │
     │                            │ OrderSseHub.register(queueId, QUEUED) │
     │                            │ KafkaTemplate.send(order-events) ────▶│
     │◀─── 202 {queueId} ─────────│  (只等待 broker ack，毫秒级)            │
     │ GET /api/orders/{id}/stream│                                       │
     │═══════ SSE 长连接 ═════════▶│                                       │
     │◀── event: QUEUED ──────────│        @KafkaListener ◀──── poll ─────│
     │◀── event: PROCESSING ──────│◀────── publish(PROCESSING)            │
     │                            │        sleep(3s) + 扣减库存/生成订单     │
     │◀── event: SUCCESS/FAILED ──│◀────── publish(结果)                   │
     │   (服务端 complete 关闭连接)  │                                       │
```

状态：`QUEUED → PROCESSING → SUCCESS | FAILED`，SSE 事件名为 `order-status`，data 为 JSON：

```json
{"queueId":"…","status":"SUCCESS","message":"下单成功，库存已扣减",
 "orderId":7,"orderNo":"20261111221242105779","totalAmount":59.00,"timestamp":"…"}
```

失败场景（如库存不足、书籍下架）推送 `FAILED` 并附带原因；失败时购物车保持不变。

## 3. 关键设计

- **Producer**（`config/KafkaConfig`）：`acks=all` + 幂等；`max.block.ms=5s`，Kafka 不可用时接口返回 503 而不是长时间挂起。
  返回 202 前等待 broker 确认，保证「已受理」的订单一定已进入队列。
- **Consumer**：`order-events` 1 个分区 + 并发 1，天然按提交顺序排队处理；手动提交位点（`enable.auto.commit=false`，由容器在处理后提交）。
- **SSE**：
  - 先登记排队单再投递消息，避免消费者先于订阅完成导致结果丢失；订阅时立即推送当前状态，若已是终态直接推送并关闭，所以迟到的订阅者也能拿到结果（结果保留 10 分钟）。
  - 只有下单用户本人可以订阅自己的排队单。
  - 浏览器 `EventSource` 无法设置请求头，SSE 接口额外支持 `?token=<JWT>`。
  - SecurityConfig 放行 `ASYNC/ERROR` 二次分派，避免 SSE 结束时被 Spring Security 拦截。
- 原有同步接口 `POST /api/v1/orders` 保持不变。

可配置项（`application.properties`）：

```properties
spring.kafka.bootstrap-servers=localhost:9092
bookstore.kafka.order-topic=order-events
bookstore.kafka.consumer-group=bookstore-order-consumer
bookstore.order.process-delay-ms=3000
bookstore.order.sse-timeout-ms=300000
```

## 4. 运行步骤

```bash
# 1) 启动单节点 Kafka
docker compose -f kafka/docker-compose.yml up -d

# 2) 启动 MySQL 后启动后端（order-events topic 启动时自动创建）
mvn spring-boot:run

# 3a) 浏览器演示（适合截图/录屏）
open http://localhost:8080/order-queue-demo.html
#    登录 → 加入购物车 → 提交订单，日志区会依次显示 202 响应与 SSE 推送；
#    处理期间点击「证明页面未阻塞」按钮可证明前端未被阻塞。

# 3b) 命令行演示
DEMO_USER=<用户名> DEMO_PASS=<密码> BOOK_ID=1 QTY=1 ./scripts/demo_async_order.sh      # 成功
DEMO_USER=<用户名> DEMO_PASS=<密码> BOOK_ID=2 QTY=9999 ./scripts/demo_async_order.sh   # 库存不足 → FAILED

# 查看 topic 中的消息
docker exec bookstore-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic order-events --from-beginning
```

## 5. 实测输出（命令行演示）

```
== 3. 提交订单（应立即返回 202 Accepted）
HTTP/1.1 202
{"code":200,"message":"订单已受理，正在排队处理","data":{"queueId":"281bd133-…","status":"QUEUED","streamUrl":"/api/orders/281bd133-…/stream"}}
POST 耗时 216 ms
== 4. 订阅 SSE /api/orders/281bd133-…/stream
[22:12:39] event:order-status
[22:12:39] data:{"queueId":"281bd133-…","status":"PROCESSING","message":"正在扣减库存…",…}
[22:12:42] event:order-status
[22:12:42] data:{"queueId":"281bd133-…","status":"SUCCESS","message":"下单成功，库存已扣减","orderId":7,…}

######## 库存不足
[22:12:45] data:{"queueId":"c512554b-…","status":"FAILED","message":"《人类简史：从动物到上帝》库存不足，剩余 79 本",…}
```

POST 在约 200ms 内返回 202，而处理结果约 3 秒后才由 SSE 推送，体现了非阻塞处理。

## 6. 新增/修改文件

新增：
- `config/KafkaConfig.java`
- `controller/AsyncOrderController.java`
- `dto/OrderEvent.java`, `dto/OrderStatusMessage.java`
- `messaging/OrderEventProducer.java`, `messaging/OrderEventConsumer.java`, `messaging/OrderSseHub.java`
- `resources/static/order-queue-demo.html`
- `kafka/docker-compose.yml`, `scripts/demo_async_order.sh`
- 测试：`messaging/OrderSseHubTest.java`，`OrderServiceImplTest` 新增 2 个用例

修改：
- `pom.xml`（`spring-kafka`）、`application.properties`（Kafka 配置）
- `OrderService` / `OrderServiceImpl`：新增 `createFromItems`，与原 `createFromCart` 共用下单逻辑
- `CartItemRepository`：新增 `deleteByUserIdAndBookIdIn`
- `JwtAuthenticationFilter`（SSE 支持 `?token=`）、`SecurityConfig`、`GlobalExceptionHandler`

## 7. 运行证据（`hw5-evidence/`）

| 文件 | 内容 | 对应评分点 |
| --- | --- | --- |
| `01-success-flow.png` | `POST /api/orders` 立即返回 **202 + queueId**，SSE 依次推送 `PROCESSING` → 约 3 秒后 `SUCCESS`（含 orderId） | KafkaProducer、SSE、状态回调、非阻塞全流程 |
| `02-failed-flow.png` | 库存不足：同样 202 立即返回，消费者处理失败后 SSE 推送 `FAILED` 及原因 | 状态回调（失败分支） |
| `03-kafka-and-console.png` | 后端控制台：Producer 在 HTTP 线程投递（partition/offset），Consumer 在独立的 `ntainer#0-0-C-1` 线程消费，并调用 `OrderSseHub` 推送；`kafka-console-consumer.sh` 读出 topic 中的订单 JSON | KafkaProducer、KafkaConsumer 异步处理 |
| `00-full-report.png` | 以上全部内容的完整版 | — |
| `demo-output.txt` / `backend-console.log` | 截图对应的原始文本输出 | — |

截图由 `scripts/hw5_render_evidence.py` 根据真实输出渲染（只读文本，不访问后端）。

复现：

```bash
docker compose -f kafka/docker-compose.yml up -d && mvn spring-boot:run
DEMO_USER=admin DEMO_PASS=admin123 BOOK_ID=1 QTY=1    ./scripts/demo_async_order.sh   # SUCCESS
DEMO_USER=admin DEMO_PASS=admin123 BOOK_ID=2 QTY=9999 ./scripts/demo_async_order.sh   # FAILED
python3 scripts/hw5_render_evidence.py
```
