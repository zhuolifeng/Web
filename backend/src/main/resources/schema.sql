-- =====================================================
-- Bookstore database schema (MySQL)
-- =====================================================

CREATE TABLE IF NOT EXISTS users (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(64)  NOT NULL,
    password     VARCHAR(128) NOT NULL,
    email        VARCHAR(128) DEFAULT NULL,
    phone        VARCHAR(32)  DEFAULT NULL,
    nickname     VARCHAR(64)  DEFAULT NULL,
    role         VARCHAR(32)  DEFAULT 'USER',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS books (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    title           VARCHAR(255)  NOT NULL,
    author          VARCHAR(128)  NOT NULL,
    category        VARCHAR(64)   DEFAULT NULL,
    badge           VARCHAR(32)   DEFAULT NULL,
    stars           VARCHAR(16)   DEFAULT NULL,
    rating_num      VARCHAR(16)   DEFAULT NULL,
    rating_count    VARCHAR(64)   DEFAULT NULL,
    price           VARCHAR(32)   DEFAULT NULL,
    original_price  VARCHAR(32)   DEFAULT NULL,
    description     TEXT,
    intro           TEXT,
    author_bio      TEXT,
    publisher       VARCHAR(128)  DEFAULT NULL,
    publish_date    VARCHAR(64)   DEFAULT NULL,
    pages           VARCHAR(32)   DEFAULT NULL,
    isbn            VARCHAR(64)   DEFAULT NULL,
    binding         VARCHAR(32)   DEFAULT NULL,
    cover_img       VARCHAR(255)  DEFAULT NULL,
    cover_emoji     VARCHAR(16)   DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       DEFAULT NULL,
    username     VARCHAR(64)  DEFAULT NULL,
    action       VARCHAR(64)  NOT NULL,
    detail       VARCHAR(512) DEFAULT NULL,
    ip_address   VARCHAR(64)  DEFAULT NULL,
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- Cart & Orders (iteration 2)
-- =====================================================

CREATE TABLE IF NOT EXISTS cart_item (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    book_id      BIGINT       NOT NULL,
    quantity     INT          NOT NULL DEFAULT 1,
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_book (user_id, book_id),
    INDEX idx_cart_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    user_id       BIGINT         NOT NULL,
    order_no      VARCHAR(64)    DEFAULT NULL,
    receiver      VARCHAR(64)    DEFAULT NULL,
    phone         VARCHAR(32)    DEFAULT NULL,
    address       VARCHAR(255)   DEFAULT NULL,
    note          VARCHAR(255)   DEFAULT NULL,
    total_amount  DECIMAL(10,2)  DEFAULT 0,
    status        VARCHAR(32)    DEFAULT 'PENDING',
    payment       VARCHAR(32)    DEFAULT NULL,
    created_at    DATETIME       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_no (order_no),
    INDEX idx_orders_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    order_id     BIGINT         NOT NULL,
    book_id      BIGINT         NOT NULL,
    title        VARCHAR(255)   DEFAULT NULL,
    author       VARCHAR(128)   DEFAULT NULL,
    price        DECIMAL(10,2)  DEFAULT 0,
    quantity     INT            NOT NULL,
    subtotal     DECIMAL(10,2)  DEFAULT 0,
    cover_img    VARCHAR(255)   DEFAULT NULL,
    cover_emoji  VARCHAR(16)    DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_oi_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
