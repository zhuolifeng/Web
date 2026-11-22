-- =====================================================
-- hw6 库存库：bookstore_inventory（库存中心 inventory-service 独占）
-- =====================================================
CREATE DATABASE IF NOT EXISTS bookstore_inventory DEFAULT CHARACTER SET utf8mb4;
USE bookstore_inventory;

DROP TABLE IF EXISTS inventory;
CREATE TABLE inventory (
    book_id     BIGINT         NOT NULL,
    title       VARCHAR(255)   NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
    stock       INT            NOT NULL,
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (book_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '书籍库存';

INSERT INTO inventory (book_id, title, price, stock) VALUES
    (1, '百年孤独',               55.00, 100),
    (2, '人类简史：从动物到上帝',  68.00, 100),
    (3, '活着',                   35.00, 100),
    (4, '三体（全集）',            168.00, 100),
    (5, '思考，快与慢',            69.00, 100);

-- Seata AT 模式必需：记录分支事务的前后镜像，用于二阶段回滚
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
