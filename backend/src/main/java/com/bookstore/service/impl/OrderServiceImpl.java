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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单业务实现。
 * <p>
 * 下单时自动扣减书籍库存；支持按日期范围和书名搜索订单。
 */
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

            // 检查并扣减库存
            if (book.getStock() < ci.getQuantity()) {
                throw new BusinessException(400, "《" + book.getTitle() + "》库存不足，剩余 " + book.getStock() + " 本");
            }
            book.setStock(book.getStock() - ci.getQuantity());
            bookRepository.save(book);

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
        // 满99免运费，否则加10元运费
        BigDecimal shipping = total.compareTo(new BigDecimal("99")) >= 0
                ? BigDecimal.ZERO : new BigDecimal("10");
        order.setTotalAmount(total.add(shipping));

        Order saved = orderRepository.save(order);
        for (OrderItem oi : items) {
            oi.setOrderId(saved.getId());
        }
        List<OrderItem> savedItems = orderItemRepository.saveAll(items);

        // 清空购物车
        cartItemRepository.deleteByUserId(userId);

        return OrderDto.from(saved, savedItems);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> listByUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return buildOrderDtoList(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getDetail(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return OrderDto.from(order, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> searchByUser(Long userId, LocalDate startDate, LocalDate endDate, String bookTitle) {
        List<Order> orders = getFilteredOrders(userId, startDate, endDate);
        List<OrderDto> dtos = buildOrderDtoList(orders);
        return filterByBookTitle(dtos, bookTitle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> searchAll(LocalDate startDate, LocalDate endDate, String bookTitle) {
        List<Order> orders = getFilteredOrders(null, startDate, endDate);
        List<OrderDto> dtos = buildOrderDtoList(orders);
        return filterByBookTitle(dtos, bookTitle);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> personalStats(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Order> orders = getFilteredOrders(userId, startDate, endDate);
        List<OrderItem> allItems = new ArrayList<>();
        for (Order o : orders) {
            allItems.addAll(orderItemRepository.findByOrderId(o.getId()));
        }

        // 按书名聚合
        Map<String, Map<String, Object>> bookStats = new LinkedHashMap<>();
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItem item : allItems) {
            String title = item.getTitle();
            Map<String, Object> stat = bookStats.computeIfAbsent(title, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("title", title);
                m.put("count", 0);
                m.put("amount", BigDecimal.ZERO);
                return m;
            });
            stat.put("count", (int) stat.get("count") + item.getQuantity());
            stat.put("amount", ((BigDecimal) stat.get("amount")).add(item.getSubtotal()));
            totalCount += item.getQuantity();
            totalAmount = totalAmount.add(item.getSubtotal());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("books", new ArrayList<>(bookStats.values()));
        result.put("totalCount", totalCount);
        result.put("totalAmount", totalAmount);
        return result;
    }

    /** 根据用户ID和日期范围获取订单，userId 为 null 时获取全部 */
    private List<Order> getFilteredOrders(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        if (userId != null && start != null && end != null) {
            return orderRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, start, end);
        } else if (userId != null && start != null) {
            return orderRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, start);
        } else if (userId != null && end != null) {
            return orderRepository.findByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(userId, end);
        } else if (userId != null) {
            return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else if (start != null && end != null) {
            return orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        } else if (start != null) {
            return orderRepository.findByCreatedAtAfterOrderByCreatedAtDesc(start);
        } else if (end != null) {
            return orderRepository.findByCreatedAtBeforeOrderByCreatedAtDesc(end);
        } else {
            return orderRepository.findAllByOrderByCreatedAtDesc();
        }
    }

    private List<OrderDto> buildOrderDtoList(List<Order> orders) {
        List<OrderDto> result = new ArrayList<>();
        for (Order o : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(o.getId());
            result.add(OrderDto.from(o, items));
        }
        return result;
    }

    /** 按书名过滤订单（保留包含匹配书籍的订单） */
    private List<OrderDto> filterByBookTitle(List<OrderDto> dtos, String bookTitle) {
        if (bookTitle == null || bookTitle.isBlank()) return dtos;
        String kw = bookTitle.trim().toLowerCase();
        return dtos.stream()
                .filter(dto -> dto.getItems().stream()
                        .anyMatch(item -> item.getTitle() != null
                                && item.getTitle().toLowerCase().contains(kw)))
                .toList();
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
