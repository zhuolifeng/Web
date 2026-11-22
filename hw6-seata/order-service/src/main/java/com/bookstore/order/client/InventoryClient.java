package com.bookstore.order.client;

import com.bookstore.order.dto.DeductRequest;
import com.bookstore.order.dto.DeductResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** 库存中心 RPC 客户端（OpenFeign）。XID 由 {@link com.bookstore.order.config.SeataFeignInterceptor} 透传。 */
@FeignClient(name = "inventory-service", url = "${inventory-service.url}")
public interface InventoryClient {

    @PostMapping("/api/inventory/deduct")
    DeductResult deduct(@RequestBody DeductRequest request);
}
