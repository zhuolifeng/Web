-- =====================================================
-- hw6 订单库：bookstore_order（订单中心 order-service 独占）
-- =====================================================
CREATE DATABASE IF NOT EXISTS bookstore_order DEFAULT CHARACTER SET utf8mb4;
USE bookstore_order;

DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    order_no     VARCHAR(64)    NOT NULL,
    user_id      BIGINT         NOT NULL,
    book_id      BIGINT         NOT NULL,
    quantity     INT            NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status       VARCHAR(32)    NOT NULL,
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_no (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单';

-- Seata AT 模式必需
DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log (
    branch_id     BIGINT       NOT NULL COMMENT 'branch transaction id',
    xid           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    context       VARCHAR(128) NOT NULL COMMENT 'undo_log context, such as serialization',
    rollback_info LONGBLOB     NOT NULL COMMENT 'rollback info',
    log_status    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    log_created   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    log_modified  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY ux_undo_log (xid, branch_id),
    KEY ix_log_created (log_created)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AT transaction mode undo table';
