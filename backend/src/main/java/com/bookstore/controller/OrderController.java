package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.dto.OrderCreateRequest;
import com.bookstore.dto.OrderDto;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.AuditService;
import com.bookstore.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuditService auditService;

    public OrderController(OrderService orderService, AuditService auditService) {
        this.orderService = orderService;
        this.auditService = auditService;
    }

    /** POST /api/v1/orders — submit a new order from current cart */
    @PostMapping
    public ApiResponse<OrderDto> create(@Valid @RequestBody OrderCreateRequest body,
                                        HttpServletRequest request) {
        Long userId = requireUserId(request);
        OrderDto dto = orderService.createFromCart(userId, body);
        String username = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        auditService.log(userId, username, "ORDER_CREATE",
                "下单 orderNo=" + dto.getOrderNo(), request);
        return ApiResponse.ok("下单成功", dto);
    }

    /** GET /api/v1/orders — current user's order list */
    @GetMapping
    public ApiResponse<List<OrderDto>> list(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return ApiResponse.ok(orderService.listByUser(userId));
    }

    /** GET /api/v1/orders/{id} — order detail */
    @GetMapping("/{id}")
    public ApiResponse<OrderDto> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        return ApiResponse.ok(orderService.getDetail(userId, id));
    }

    private Long requireUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Long)) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return (Long) attr;
    }
}
