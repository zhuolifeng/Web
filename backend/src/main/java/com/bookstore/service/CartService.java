package com.bookstore.service;

import com.bookstore.dto.CartItemDto;
import com.bookstore.entity.CartItem;

import java.util.List;

/**
 * 购物车业务接口。
 * 提供加入、更新数量、移除和清空购物车等操作。
 */
public interface CartService {

    List<CartItemDto> listByUser(Long userId);

    CartItem add(Long userId, Long bookId, Integer quantity);

    CartItem updateQuantity(Long userId, Long cartItemId, Integer quantity);

    void remove(Long userId, Long cartItemId);

    void clear(Long userId);
}
