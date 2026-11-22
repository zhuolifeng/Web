package com.bookstore.order.controller;

import com.bookstore.order.dto.OrderResult;
import com.bookstore.order.dto.PlaceOrderRequest;
import com.bookstore.order.repository.OrderRepository;
import com.bookstore.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    /** 统一下单接口（Seata AT 全局事务）。body: {"userId":1,"bookId":1,"quantity":2,"simulateError":false} */
    @PostMapping
    public OrderResult placeOrder(@RequestBody PlaceOrderRequest req) {
        return orderService.placeOrder(req);
    }

    /** 对照组：不开启全局事务，演示异常时库存无法回滚。 */
    @PostMapping("/no-global-tx")
    public OrderResult placeOrderWithoutGlobalTx(@RequestBody PlaceOrderRequest req) {
        return orderService.placeOrderWithoutGlobalTx(req);
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return orderRepository.findAll();
    }
}
