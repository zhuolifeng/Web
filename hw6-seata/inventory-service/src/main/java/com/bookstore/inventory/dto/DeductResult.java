package com.bookstore.inventory.dto;

import java.math.BigDecimal;

/** 扣减库存结果：返回单价供订单中心计算金额。 */
public record DeductResult(Long bookId, String title, BigDecimal price, Integer deducted, Integer remainingStock) {
}
