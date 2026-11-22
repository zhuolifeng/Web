package com.bookstore.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 库存中心（Seata RM）。
 * <p>
 * 只连接 bookstore_inventory 库；数据源由 seata-spring-boot-starter 自动代理为 DataSourceProxy（AT 模式），
 * 扣减库存时自动生成 undo_log，全局事务回滚时由 TC 通知本服务按前镜像恢复库存。
 */
@SpringBootApplication
public class InventoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
