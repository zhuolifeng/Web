# 猪猪书城 - 后端 Spring Boot 工程

迭代 2（作业 4）的后端工程，与前端 React 工程（位于上一级目录）配套使用。

## 技术栈

- Spring Boot 3.2.5
- Spring Web (REST)
- Spring Data JPA + Hibernate
- MySQL 8.x
- Maven, Java 17
- Lombok

## 工程结构（分层架构）

```
backend/
├── pom.xml
└── src/main/
    ├── java/com/bookstore/
    │   ├── BookstoreApplication.java        ← Spring Boot 启动类
    │   ├── config/
    │   │   └── WebConfig.java               ← CORS 配置
    │   ├── controller/                      ← 表现层 (REST API)
    │   │   ├── BookController.java
    │   │   └── UserController.java
    │   ├── service/                         ← 业务层
    │   │   ├── BookService.java
    │   │   └── UserService.java
    │   ├── repository/                      ← 数据访问层 (Spring Data JPA)
    │   │   ├── BookRepository.java
    │   │   └── UserRepository.java
    │   ├── entity/                          ← 实体类
    │   │   ├── Book.java
    │   │   └── User.java
    │   ├── dto/                             ← 数据传输对象
    │   │   ├── ApiResponse.java
    │   │   └── UserRegisterRequest.java
    │   └── exception/                       ← 异常处理
    │       ├── BusinessException.java
    │       └── GlobalExceptionHandler.java
    └── resources/
        ├── application.properties           ← MySQL 等配置
        ├── schema.sql                       ← 建表脚本
        └── data.sql                         ← 样例数据
```

## 数据库准备

在 MySQL 中执行：

```sql
CREATE DATABASE IF NOT EXISTS bookstore DEFAULT CHARACTER SET utf8mb4;
```

随后修改 `src/main/resources/application.properties` 中的 `spring.datasource.username`
和 `spring.datasource.password` 为本机 MySQL 帐号。首次启动时，
`schema.sql` 会建表，`data.sql` 会写入 8 条样例书籍。

## 启动

```bash
cd backend
mvn spring-boot:run
```

默认监听 `http://localhost:8080`。

## REST API

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/api/v1/users/register` | 用户注册，写入 `users` 表 |
| GET  | `/api/v1/books`         | 查询全部书籍 |
| GET  | `/api/v1/book/{id}`     | 查询单本书籍详情 |

### Postman 示例

1. **POST** `http://localhost:8080/api/v1/users/register`
   Body (JSON):
   ```json
   {
     "username": "alice",
     "password": "secret123",
     "email": "alice@example.com",
     "phone": "13800000000",
     "nickname": "Alice"
   }
   ```
   预期：`201 Created`，返回新建用户。

2. **GET** `http://localhost:8080/api/v1/books`
   预期：`200 OK`，返回 books 数组。

3. **GET** `http://localhost:8080/api/v1/book/4`
   预期：`200 OK`，返回《三体（全集）》。
