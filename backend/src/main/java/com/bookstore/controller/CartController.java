package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.dto.CartAddRequest;
import com.bookstore.dto.CartItemDto;
import com.bookstore.dto.CartUpdateRequest;
import com.bookstore.entity.CartItem;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** GET /api/v1/cart — current user's cart */
    @GetMapping
    public ApiResponse<List<CartItemDto>> list(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return ApiResponse.ok(cartService.listByUser(userId));
    }

    /** POST /api/v1/cart — add a book to the cart */
    @PostMapping
    public ApiResponse<CartItem> add(@Valid @RequestBody CartAddRequest body,
                                     HttpServletRequest request) {
        Long userId = requireUserId(request);
        return ApiResponse.ok("已加入购物车",
                cartService.add(userId, body.getBookId(), body.getQuantity()));
    }

    /** PUT /api/v1/cart/{id} — change quantity */
    @PutMapping("/{id}")
    public ApiResponse<CartItem> update(@PathVariable Long id,
                                        @Valid @RequestBody CartUpdateRequest body,
                                        HttpServletRequest request) {
        Long userId = requireUserId(request);
        return ApiResponse.ok(cartService.updateQuantity(userId, id, body.getQuantity()));
    }

    /** DELETE /api/v1/cart/{id} — remove one cart item */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        cartService.remove(userId, id);
        return ApiResponse.ok("已删除", null);
    }

    /** DELETE /api/v1/cart — clear cart */
    @DeleteMapping
    public ApiResponse<Void> clear(HttpServletRequest request) {
        Long userId = requireUserId(request);
        cartService.clear(userId);
        return ApiResponse.ok("已清空购物车", null);
    }

    private Long requireUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Long)) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return (Long) attr;
    }
}
