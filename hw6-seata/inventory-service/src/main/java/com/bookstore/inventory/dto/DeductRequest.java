package com.bookstore.inventory.dto;

/** 扣减库存请求。 */
public record DeductRequest(Long bookId, Integer quantity) {
}
