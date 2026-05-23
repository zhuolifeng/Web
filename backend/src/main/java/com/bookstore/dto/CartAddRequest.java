package com.bookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartAddRequest {

    @NotNull(message = "bookId 不能为空")
    private Long bookId;

    @NotNull
    @Min(value = 1, message = "数量必须 >= 1")
    private Integer quantity;

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
