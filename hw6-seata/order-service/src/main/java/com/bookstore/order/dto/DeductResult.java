package com.bookstore.order.dto;

import java.math.BigDecimal;

public record DeductResult(Long bookId, String title, BigDecimal price, Integer deducted, Integer remainingStock) {
}
