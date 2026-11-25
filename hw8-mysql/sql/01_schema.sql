-- =====================================================================
-- hw8：书店核心库表 DDL（MySQL 8.x / InnoDB / utf8mb4）
--   users        用户表
--   books        图书表
--   orders       订单主表   —— 反范式：冗余 user_nickname、book_snapshot、item_count
--   order_items  订单详情表 —— 反范式：冗余下单时刻的图书快照（书名/作者/ISBN/封面/成交单价）
-- 说明：高并发 OLTP 场景下不建物理外键（避免加锁与级联开销），由应用层保证引用完整性，
--       但所有“逻辑外键”列都建了索引。
-- =====================================================================
DROP DATABASE IF EXISTS bookstore_hw8;
CREATE DATABASE bookstore_hw8 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE bookstore_hw8;

-- ---------------------------------------------------------------- 用户表
CREATE TABLE users (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    username       VARCHAR(64)      NOT NULL                COMMENT '登录名',
    password_hash  CHAR(60)         NOT NULL                COMMENT 'BCrypt 哈希，定长 60',
    nickname       VARCHAR(64)      NOT NULL                COMMENT '昵称（会被冗余到 orders.user_nickname）',
    email          VARCHAR(128)     DEFAULT NULL,
    phone          VARCHAR(20)      DEFAULT NULL,
    role           ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    enabled        TINYINT(1)       NOT NULL DEFAULT 1,
    created_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB COMMENT='用户表';

-- ---------------------------------------------------------------- 图书表
CREATE TABLE books (
    id              BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键',
    isbn            VARCHAR(17)       NOT NULL                COMMENT 'ISBN-13（含连字符最长 17）',
    title           VARCHAR(255)      NOT NULL,
    author          VARCHAR(128)      NOT NULL,
    category        VARCHAR(32)       NOT NULL                COMMENT '分类（复合索引第 1 列）',
    publisher       VARCHAR(128)      DEFAULT NULL,
    publish_date    DATE              DEFAULT NULL,
    price           DECIMAL(10,2)     NOT NULL                COMMENT '售价：定点数，避免浮点误差（复合索引第 2 列）',
    original_price  DECIMAL(10,2)     DEFAULT NULL,
    stock           INT UNSIGNED      NOT NULL DEFAULT 0,
    sales           INT UNSIGNED      NOT NULL DEFAULT 0,
    rating          DECIMAL(2,1)      DEFAULT NULL            COMMENT '0.0 ~ 5.0',
    cover_img       VARCHAR(255)      DEFAULT NULL,
    description     TEXT              DEFAULT NULL,
    status          TINYINT           NOT NULL DEFAULT 1      COMMENT '1 上架 0 下架',
    created_at      DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_books_isbn (isbn)
    -- 复合索引 idx_category_price(category, price) 在 03_index.sql 中创建，便于对比建索引前后的执行计划
) ENGINE=InnoDB COMMENT='图书表';

-- ---------------------------------------------------------------- 订单主表（反范式）
CREATE TABLE orders (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no       CHAR(20)         NOT NULL                COMMENT '业务单号 yyyyMMddHHmmss + 6 位随机',
    user_id        BIGINT UNSIGNED  NOT NULL                COMMENT '逻辑外键 → users.id',
    -- ↓↓↓ 反范式冗余字段（空间换时间）↓↓↓
    user_nickname  VARCHAR(64)      NOT NULL                COMMENT '【冗余】下单时用户昵称，订单列表/后台无需 JOIN users',
    item_count     INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT '【冗余】商品总件数，列表无需 SUM(order_items.quantity)',
    book_snapshot  JSON             NOT NULL                COMMENT '【冗余】图书快照 [{bookId,title,cover,price,qty}]，列表直接展示，无需 JOIN order_items/books',
    -- ↑↑↑
    total_amount   DECIMAL(10,2)    NOT NULL DEFAULT 0.00,
    status         ENUM('PENDING','PAID','SHIPPED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    receiver       VARCHAR(64)      NOT NULL,
    phone          VARCHAR(20)      NOT NULL,
    address        VARCHAR(255)     NOT NULL,
    created_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_no (order_no),
    KEY idx_orders_user_created (user_id, created_at)   -- “我的订单”按时间倒序分页
) ENGINE=InnoDB COMMENT='订单主表';

-- ---------------------------------------------------------------- 订单详情表（快照）
CREATE TABLE order_items (
    id           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id     BIGINT UNSIGNED  NOT NULL                COMMENT '逻辑外键 → orders.id',
    book_id      BIGINT UNSIGNED  NOT NULL                COMMENT '逻辑外键 → books.id',
    -- ↓↓↓ 下单时刻的图书快照：图书改价/改名/下架后，历史订单仍保持原样 ↓↓↓
    book_title   VARCHAR(255)     NOT NULL,
    book_author  VARCHAR(128)     NOT NULL,
    book_isbn    VARCHAR(17)      NOT NULL,
    cover_img    VARCHAR(255)     DEFAULT NULL,
    unit_price   DECIMAL(10,2)    NOT NULL                COMMENT '成交单价（不是 books.price 的当前价）',
    -- ↑↑↑
    quantity     INT UNSIGNED     NOT NULL,
    subtotal     DECIMAL(10,2)    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_items_order (order_id),
    KEY idx_items_book (book_id)
) ENGINE=InnoDB COMMENT='订单详情表';
