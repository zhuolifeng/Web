package com.bookstore.service;

import com.bookstore.dto.OrderCreateRequest;
import com.bookstore.dto.OrderDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 订单业务接口。
 * <p>
 * 提供普通用户与管理员两套查询方式，均返回 {@link OrderDto}。
 */
public interface OrderService {

    /** 从购物车创建订单，同时扣减库存 */
    OrderDto createFromCart(Long userId, OrderCreateRequest request);

    /** 查询某用户的全部订单 */
    List<OrderDto> listByUser(Long userId);

    /** 查询某用户的订单详情 */
    OrderDto getDetail(Long userId, Long orderId);

    /** 按条件搜索当前用户的订单（日期范围 + 书名） */
    List<OrderDto> searchByUser(Long userId, LocalDate startDate, LocalDate endDate, String bookTitle);

    /** 管理员查看全部订单（支持日期范围 + 书名过滤） */
    List<OrderDto> searchAll(LocalDate startDate, LocalDate endDate, String bookTitle);

    /** 顾客个人购书统计（指定时间范围内每种书的购买数量、总本数、总金额） */
    Map<String, Object> personalStats(Long userId, LocalDate startDate, LocalDate endDate);
}
