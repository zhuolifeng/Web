# 作业 6：订单中心 / 库存中心拆库 + Seata AT 分布式事务

## 1. 需求与实现对照

| 作业要求 | 实现 |
| --- | --- |
| 两套逻辑分离的数据库 | 同一 MySQL 实例中的 `bookstore_inventory`（库存库）与 `bookstore_order`（订单库），见 `sql/01_inventory_db.sql`、`sql/02_order_db.sql` |
| 集成 Apache Seata（AT 模式） | Seata Server 2.1.0（TC，file 模式，`:8091`）+ `seata-spring-boot-starter 2.1.0`；两个服务均配置 `data-source-proxy-mode: AT`，各库建有 `undo_log` 表 |
| 统一下单接口，RPC 先扣库存再生成订单 | `order-service` 的 `POST /api/orders`（`@GlobalTransactional`）→ OpenFeign `InventoryClient` 调 `inventory-service` 的 `POST /api/inventory/deduct` → 写 `bookstore_order.orders` |
| 订单入库时模拟 RuntimeException | 请求体 `"simulateError": true` 时，`OrderRepository#insert` 在 INSERT 执行**之后**抛出 `RuntimeException` |
| 验证库存自动回滚 | `scripts/verify.sh` 自动对比数据库前后状态并断言；日志与截图见 `evidence/` |

## 2. 架构

```
                       ┌──────────────── Seata Server (TC, :8091, file 模式) ───────────────┐
                       │      ① begin → XID       ④ rollback(XID) → 通知各分支二阶段回滚      │
                       └───────▲─────────────────────────▲───────────────────▲──────────────┘
                               │ TM                      │ RM 注册分支 1      │ RM 注册分支 2
 client ── POST /api/orders ──▶ order-service (:9002) ── ② OpenFeign + Header TX_XID ──▶ inventory-service (:9001)
                               │ @GlobalTransactional                                  │ UPDATE inventory SET stock=stock-?
                               │ ③ INSERT orders → throw RuntimeException(模拟)         │ + undo_log（前/后镜像）
                               ▼                                                       ▼
                       MySQL: bookstore_order (orders, undo_log)        MySQL: bookstore_inventory (inventory, undo_log)
```

XID 透传：
- 调用方 `order-service/.../config/SeataFeignInterceptor`：把 `RootContext.getXID()` 写入请求头 `TX_XID`。
- 被调用方 `inventory-service/.../config/SeataXidFilter`：读取 `TX_XID` 并 `RootContext.bind(xid)`，请求结束后 `unbind`。

AT 模式两阶段：
1. **一阶段**：DataSourceProxy 拦截 SQL，查询前镜像 → 执行业务 SQL → 查询后镜像 → 写 `undo_log`，向 TC 注册分支并拿全局锁，**本地事务直接提交**。
2. **二阶段回滚**：TM 捕获异常后通知 TC 全局回滚，TC 回调各 RM，RM 校验后镜像与当前数据一致后，用前镜像生成反向 SQL 恢复数据并删除 `undo_log`。
   **二阶段提交**：仅异步删除 `undo_log`。

## 3. 目录

```
hw6-seata/
├── pom.xml                         # 父 POM：Spring Boot 3.2.5 / Spring Cloud 2023.0.3 / Seata 2.1.0
├── inventory-service/              # 库存中心（RM），:9001，库 bookstore_inventory
│   ├── .../service/InventoryService.java     # 扣减库存（分支事务）
│   ├── .../config/SeataXidFilter.java        # 入站绑定 XID
│   └── resources/application.yml             # 数据源 + Seata 客户端配置
├── order-service/                  # 订单中心（TM + RM），:9002，库 bookstore_order
│   ├── .../service/OrderService.java         # @GlobalTransactional 统一下单 + 无全局事务对照组
│   ├── .../client/InventoryClient.java       # OpenFeign RPC
│   ├── .../config/SeataFeignInterceptor.java # 出站透传 XID
│   ├── .../repository/OrderRepository.java   # 订单入库 + 模拟异常
│   └── resources/application.yml
├── sql/                            # 两个库的建表 + undo_log
├── scripts/                        # init-db / start-all / stop-all / verify
└── evidence/                       # 验证输出、控制台日志、截图
```

## 4. 运行

前置：JDK 17+、Maven、MySQL 8（默认 `root/123`，可用环境变量 `MYSQL_USER`/`MYSQL_PASSWORD` 覆盖）。

```bash
cd hw6-seata
# 1) 下载 Seata Server 2.1.0 并解压到 seata-server-dist/（或设置 SEATA_HOME 指向已有的 seata-server 目录）
mkdir -p seata-server-dist && cd seata-server-dist
curl -LO https://archive.apache.org/dist/incubator/seata/2.1.0/apache-seata-2.1.0-incubating-bin.tar.gz
tar xzf apache-seata-2.1.0-incubating-bin.tar.gz && cd ..

# 2) 初始化两个库、编译
./scripts/init-db.sh
mvn -DskipTests package

# 3) 后台启动 TC + 两个服务（日志：logs/*.log）
./scripts/start-all.sh

# 4) 一键验证（正常 / 异常 / 对照组），输出到 evidence/
./scripts/verify.sh

# 5) 停止
./scripts/stop-all.sh
```

手动调用：

```bash
# 正常下单
curl -s -H 'Content-Type: application/json' -d '{"userId":1,"bookId":1,"quantity":2}' localhost:9002/api/orders
# 订单入库时模拟异常（库存应自动回滚）
curl -s -H 'Content-Type: application/json' -d '{"userId":1,"bookId":2,"quantity":3,"simulateError":true}' localhost:9002/api/orders
# 对照组：无全局事务
curl -s -H 'Content-Type: application/json' -d '{"userId":1,"bookId":3,"quantity":5,"simulateError":true}' localhost:9002/api/orders/no-global-tx
# 查询
curl -s localhost:9001/api/inventory ; curl -s localhost:9002/api/orders
```

## 5. 验证结果

环境：macOS、JDK 24、MySQL 8.4、Seata Server 2.1.0（file 模式）。`./scripts/verify.sh` 结果 **PASS=8 FAIL=0**：

| 用例 | 请求 | 库存（前 → 后） | 订单数（前 → 后） | 结论 |
| --- | --- | --- | --- | --- |
| 1 正常流程 | `bookId=1, quantity=2` | 100 → **98** | 0 → **1** | 扣减与下单同时成功，全局提交后 undo_log 被异步清理 |
| 2 异常流程 | `bookId=2, quantity=3, simulateError=true` | 100 → **100** | 1 → **1** | HTTP 500；库存中心一阶段已扣成 97 并提交，Seata 二阶段按 undo_log 恢复为 100；订单 INSERT 同样被撤销 |
| 对照组 | `/no-global-tx`，`bookId=3, quantity=5, simulateError=true` | 100 → **95** | 1 → **1** | 只有本地事务：订单回滚了，库存却少了 5 → 数据不一致，说明需要分布式事务 |

异常流程关键日志（同一 XID 贯穿三个进程，完整内容见 `evidence/rollback-console.log`）：

```
[order-service]     Begin new global transaction [198.18.0.1:8091:...122]
[order-service]     [Seata] Feign 调用 /api/inventory/deduct 透传 XID=...122
[inventory-service] [Seata] 收到上游全局事务 XID=...122，绑定到当前线程
[inventory-service] [库存中心] 扣减成功（一阶段已提交，undo_log 已写入） bookId=2 剩余库存=97
[inventory-service] branch register success, branchId:...123, lockKeys:inventory:2
[order-service]     branch register success, branchId:...124, lockKeys:orders:2
[order-service]     [订单中心] >>> 模拟订单入库异常：即将抛出 RuntimeException
[order-service]     transaction ...122 will be rollback
[seata-server]      Rollback branch transaction successfully ... branchId = ...124 (bookstore_order)
[inventory-service] Branch Rollbacking ... jdbc:mysql://localhost:3306/bookstore_inventory
[inventory-service] branch rollback success ... branchId:...123
[seata-server]      Rollback global transaction successfully, xid = ...122
[order-service]     [...122] rollback status: Rollbacked
```

证据文件（`evidence/`）：

| 文件 | 内容 |
| --- | --- |
| `db-before-after.png` | 异常流程数据库前后对比截图（库存、订单、undo_log） |
| `rollback-console.png` | 异常流程三个进程的控制台日志截图 |
| `verify-report.png` | 全部用例输出截图 |
| `verify-output.txt` / `rollback-console.log` | 上述截图对应的原始文本 |

截图由 `python3 scripts/render_evidence.py` 把 verify.sh 的实际输出渲染为 HTML 后用本机 Chrome headless 截取。

## 6. 说明与注意点

- **为什么不在 `placeOrder` 上叠加 `@Transactional`**：AT 模式下每条 SQL 在 DataSourceProxy 中以分支事务提交并写 undo_log，异常时由 Seata 统一回滚；
  不加本地事务也避免了在 RPC 调用期间长时间占用订单库连接，并规避 `@GlobalTransactional` 与 `@Transactional` 的拦截器顺序问题。
- **XID 透传自己实现**：只引入 `seata-spring-boot-starter`，不依赖 spring-cloud-alibaba，透传逻辑在 Feign 拦截器 + Servlet Filter 中一目了然。
- **库存不足**：库存中心返回 409，Feign 抛出异常，全局事务回滚，订单中心返回 409。
- **两个库都要有 `undo_log` 表**，表结构为 Seata 官方 MySQL 版本。
- 单元测试：`order-service` 中 `SeataFeignInterceptorTest` 验证有/无全局事务时 XID 头的透传（`mvn test`）。

## 7. 提交打包

```bash
./scripts/package.sh   # 生成 ../hw6-seata-submission.zip，排除 target/、logs/、seata-server-dist/ 等第三方/生成文件
```
