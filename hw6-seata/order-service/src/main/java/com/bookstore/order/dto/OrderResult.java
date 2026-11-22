package com.bookstore.order.dto;

import java.math.BigDecimal;

public record OrderResult(String xid, Long orderId, String orderNo, Long bookId, String title,
                          Integer quantity, BigDecimal totalAmount, Integer remainingStock) {
}
