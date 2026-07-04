package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.dto.OrderDto;
import com.bookstore.dto.UserDto;
import com.bookstore.service.OrderService;
import com.bookstore.service.StatisticsService;
import com.bookstore.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 管理员专用 REST 接口（/api/v1/admin/**）。
 * <p>
 * Spring Security 在 {@link com.bookstore.config.SecurityConfig} 中对该路径
 * 统一要求 {@code ROLE_ADMIN}，Controller 无需再做角色判断。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;
    private final StatisticsService statisticsService;

    public AdminController(UserService userService,
                           OrderService orderService,
                           StatisticsService statisticsService) {
        this.userService = userService;
        this.orderService = orderService;
        this.statisticsService = statisticsService;
    }

    // ==================== 用户管理 ====================

    /** GET /api/v1/admin/users — 查看所有用户 */
    @GetMapping("/users")
    public ApiResponse<List<UserDto>> listUsers() {
        return ApiResponse.ok(userService.listAll());
    }

    /** PUT /api/v1/admin/users/{id}/status — 禁用/解禁用户 */
    @PutMapping("/users/{id}/status")
    public ApiResponse<UserDto> toggleUserStatus(@PathVariable Long id,
                                                  @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return ApiResponse.ok(userService.toggleEnabled(id, enabled));
    }

    // ==================== 订单管理（管理员查看全部） ====================

    /** GET /api/v1/admin/orders — 查看所有订单（支持日期和书名过滤） */
    @GetMapping("/orders")
    public ApiResponse<List<OrderDto>> listAllOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String bookTitle) {
        return ApiResponse.ok(orderService.searchAll(startDate, endDate, bookTitle));
    }

    // ==================== 统计 ====================

    /** GET /api/v1/admin/statistics/book-sales — 书籍销量排行 */
    @GetMapping("/statistics/book-sales")
    public ApiResponse<List<Map<String, Object>>> bookSalesRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(statisticsService.bookSalesRanking(startDate, endDate));
    }

    /** GET /api/v1/admin/statistics/user-spending — 用户消费排行 */
    @GetMapping("/statistics/user-spending")
    public ApiResponse<List<Map<String, Object>>> userSpendingRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(statisticsService.userSpendingRanking(startDate, endDate));
    }
}
