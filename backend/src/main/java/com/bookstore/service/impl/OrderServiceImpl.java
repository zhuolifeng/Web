package com.bookstore.service.impl;

import com.bookstore.dto.OrderCreateRequest;
import com.bookstore.dto.OrderDto;
import com.bookstore.entity.Book;
import com.bookstore.entity.CartItem;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import com.bookstore.exception.BusinessException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.repository.OrderItemRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            CartItemRepository cartItemRepository,
                            BookRepository bookRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional
    public OrderDto createFromCart(Long userId, OrderCreateRequest req) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(400, "购物车为空，无法下单");
        }

        List<Long> bookIds = cartItems.stream().map(CartItem::getBookId).toList();
        Map<Long, Book> bookMap = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, b -> b));

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setReceiver(req.getReceiver());
        order.setPhone(req.getPhone());
        order.setAddress(joinAddress(req));
        order.setNote(req.getNote());
        order.setPayment(req.getPayment());
        order.setStatus("PAID");
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (CartItem ci : cartItems) {
            Book book = bookMap.get(ci.getBookId());
            if (book == null) continue;
            BigDecimal unit = parsePrice(book.getPrice());
            BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(ci.getQuantity()));
            OrderItem oi = new OrderItem();
            oi.setBookId(book.getId());
            oi.setTitle(book.getTitle());
            oi.setAuthor(book.getAuthor());
            oi.setPrice(unit);
            oi.setQuantity(ci.getQuantity());
            oi.setSubtotal(subtotal);
            oi.setCoverImg(book.getCoverImg());
            oi.setCoverEmoji(book.getCoverEmoji());
            items.add(oi);
            total = total.add(subtotal);
        }
        // shipping: free if subtotal >= 99 else 10
        BigDecimal shipping = total.compareTo(new BigDecimal("99")) >= 0
                ? BigDecimal.ZERO : new BigDecimal("10");
        order.setTotalAmount(total.add(shipping));

        Order saved = orderRepository.save(order);
        for (OrderItem oi : items) {
            oi.setOrderId(saved.getId());
        }
        List<OrderItem> savedItems = orderItemRepository.saveAll(items);

        // clear cart
        cartItemRepository.deleteByUserId(userId);

        return OrderDto.from(saved, savedItems);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> listByUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<OrderDto> result = new ArrayList<>();
        for (Order o : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(o.getId());
            result.add(OrderDto.from(o, items));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getDetail(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return OrderDto.from(order, items);
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(ORDER_NO_FMT)
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String joinAddress(OrderCreateRequest req) {
        StringBuilder sb = new StringBuilder();
        if (req.getProvince() != null) sb.append(req.getProvince());
        if (req.getCity() != null) sb.append(" ").append(req.getCity());
        if (req.getAddress() != null) sb.append(" ").append(req.getAddress());
        return sb.toString().trim();
    }

    private BigDecimal parsePrice(String text) {
        if (text == null) return BigDecimal.ZERO;
        String cleaned = text.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(cleaned);
    }
}
