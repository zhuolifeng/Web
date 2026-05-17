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
