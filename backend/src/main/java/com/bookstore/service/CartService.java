package com.bookstore.service;

import com.bookstore.dto.CartItemDto;
import com.bookstore.entity.CartItem;

import java.util.List;

public interface CartService {

    List<CartItemDto> listByUser(Long userId);

    CartItem add(Long userId, Long bookId, Integer quantity);

    CartItem updateQuantity(Long userId, Long cartItemId, Integer quantity);

    void remove(Long userId, Long cartItemId);

    void clear(Long userId);
}
