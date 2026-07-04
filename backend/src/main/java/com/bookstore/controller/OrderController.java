package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.dto.OrderCreateRequest;
import com.bookstore.dto.OrderDto;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.AuditService;
import com.bookstore.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 订单相关 REST 接口。
 * <p>
 * 普通用户可查看/搜索自己的订单，以及查看个人购书统计。
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuditService auditService;

    public OrderController(OrderService orderService, AuditService auditService) {
        this.orderService = orderService;
        this.auditService = auditService;
    }

    /** POST /api/v1/orders — 从购物车下单 */
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

    /** GET /api/v1/orders — 当前用户的订单列表（支持日期和书名过滤） */
    @GetMapping
    public ApiResponse<List<OrderDto>> list(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String bookTitle) {
        Long userId = requireUserId(request);
        if (startDate != null || endDate != null || (bookTitle != null && !bookTitle.isBlank())) {
            return ApiResponse.ok(orderService.searchByUser(userId, startDate, endDate, bookTitle));
        }
        return ApiResponse.ok(orderService.listByUser(userId));
    }

    /** GET /api/v1/orders/{id} — 订单详情 */
    @GetMapping("/{id}")
    public ApiResponse<OrderDto> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        return ApiResponse.ok(orderService.getDetail(userId, id));
    }

    /** GET /api/v1/orders/my-stats — 当前用户的购书统计 */
    @GetMapping("/my-stats")
    public ApiResponse<Map<String, Object>> personalStats(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = requireUserId(request);
        return ApiResponse.ok(orderService.personalStats(userId, startDate, endDate));
    }

    private Long requireUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Long)) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return (Long) attr;
    }
}
