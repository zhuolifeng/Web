package com.bookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartUpdateRequest {

    @NotNull
    @Min(value = 1, message = "数量必须 >= 1")
    private Integer quantity;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
