package com.bookstore.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单中心（Seata TM + RM）。
 * <p>
 * 提供统一下单接口：开启全局事务 → OpenFeign 调用库存中心扣减库存 → 在 bookstore_order 库生成订单。
 */
@SpringBootApplication
@EnableFeignClients
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
