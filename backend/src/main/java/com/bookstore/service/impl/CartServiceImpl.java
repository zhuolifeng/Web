package com.bookstore.service.impl;

import com.bookstore.dto.CartItemDto;
import com.bookstore.entity.Book;
import com.bookstore.entity.CartItem;
import com.bookstore.exception.BusinessException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository, BookRepository bookRepository) {
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemDto> listByUser(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (items.isEmpty()) return new ArrayList<>();

        List<Long> bookIds = items.stream().map(CartItem::getBookId).toList();
        Map<Long, Book> bookMap = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, b -> b));

        List<CartItemDto> result = new ArrayList<>();
        for (CartItem ci : items) {
            Book book = bookMap.get(ci.getBookId());
            if (book == null) continue;
            result.add(toDto(ci, book));
        }
        return result;
    }

    @Override
    @Transactional
    public CartItem add(Long userId, Long bookId, Integer quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(404, "书籍不存在: id=" + bookId));
        int qty = quantity == null ? 1 : Math.max(1, quantity);

        return cartItemRepository.findByUserIdAndBookId(userId, bookId)
                .map(existing -> {
                    existing.setQuantity(Math.min(99, existing.getQuantity() + qty));
                    return cartItemRepository.save(existing);
                })
                .orElseGet(() -> cartItemRepository.save(
                        new CartItem(userId, book.getId(), Math.min(99, qty))));
    }

    @Override
    @Transactional
    public CartItem updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new BusinessException(404, "购物车条目不存在"));
        if (quantity == null || quantity < 1) {
            throw new BusinessException(400, "数量必须 >= 1");
        }
        item.setQuantity(Math.min(99, quantity));
        return cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new BusinessException(404, "购物车条目不存在"));
        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clear(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    private CartItemDto toDto(CartItem ci, Book book) {
        CartItemDto dto = new CartItemDto();
        dto.setId(ci.getId());
        dto.setBookId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setPrice(book.getPrice());
        dto.setOriginalPrice(book.getOriginalPrice());
        dto.setCoverImg(book.getCoverImg());
        dto.setCoverEmoji(book.getCoverEmoji());
        dto.setQuantity(ci.getQuantity());
        BigDecimal unit = parsePrice(book.getPrice());
        dto.setUnitPrice(unit);
        dto.setSubtotal(unit.multiply(BigDecimal.valueOf(ci.getQuantity())));
        return dto;
    }

    private BigDecimal parsePrice(String text) {
        if (text == null) return BigDecimal.ZERO;
        String cleaned = text.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(cleaned);
    }
}
