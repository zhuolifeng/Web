package com.bookstore.order.dto;

public record DeductRequest(Long bookId, Integer quantity) {
}
