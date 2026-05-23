package com.bookstore.service;

import com.bookstore.dto.OrderCreateRequest;
import com.bookstore.dto.OrderDto;

import java.util.List;

public interface OrderService {

    OrderDto createFromCart(Long userId, OrderCreateRequest request);

    List<OrderDto> listByUser(Long userId);

    OrderDto getDetail(Long userId, Long orderId);
}
