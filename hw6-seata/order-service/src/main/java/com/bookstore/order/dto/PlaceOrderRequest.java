package com.bookstore.order.dto;

/**
 * 统一下单请求。
 *
 * @param simulateError true 时在订单入库后抛出 RuntimeException，用于验证分布式回滚
 */
public record PlaceOrderRequest(Long userId, Long bookId, Integer quantity, Boolean simulateError) {

    public boolean shouldFail() {
        return Boolean.TRUE.equals(simulateError);
    }
}
