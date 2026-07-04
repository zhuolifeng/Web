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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderServiceImpl 单元测试。
 * 覆盖下单、查询、搜索、个人统计等核心业务逻辑。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Book sampleBook;
    private CartItem sampleCartItem;

    @BeforeEach
    void setUp() {
        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("三体");
        sampleBook.setAuthor("刘慈欣");
        sampleBook.setPrice("¥89.00");
        sampleBook.setStock(60);

        sampleCartItem = new CartItem();
        sampleCartItem.setId(1L);
        sampleCartItem.setUserId(1L);
        sampleCartItem.setBookId(1L);
        sampleCartItem.setQuantity(2);
    }

    @Test
    @DisplayName("下单 - 购物车为空时抛出异常")
    void testCreateFromCartEmpty() {
        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        OrderCreateRequest req = new OrderCreateRequest();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createFromCart(1L, req));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("购物车为空"));
    }

    @Test
    @DisplayName("下单 - 库存不足时抛出异常")
    void testCreateFromCartInsufficientStock() {
        sampleBook.setStock(1);
        sampleCartItem.setQuantity(5);

        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.singletonList(sampleCartItem));
        when(bookRepository.findAllById(anyList()))
                .thenReturn(Collections.singletonList(sampleBook));

        OrderCreateRequest req = new OrderCreateRequest();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createFromCart(1L, req));
        assertTrue(ex.getMessage().contains("库存不足"));
    }

    @Test
    @DisplayName("下单 - 成功下单并扣减库存、清空购物车")
    void testCreateFromCartSuccess() {
        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.singletonList(sampleCartItem));
        when(bookRepository.findAllById(anyList()))
                .thenReturn(Collections.singletonList(sampleBook));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCreateRequest req = new OrderCreateRequest();
        req.setReceiver("张三");
        req.setPhone("13800138000");

        OrderDto result = orderService.createFromCart(1L, req);

        assertNotNull(result);
        // 验证库存被扣减（原60，购买2本）
        assertEquals(58, sampleBook.getStock());
        // 验证购物车被清空
        verify(cartItemRepository).deleteByUserId(1L);
        verify(bookRepository).save(sampleBook);
    }

    @Test
    @DisplayName("查询用户订单列表")
    void testListByUser() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setOrderNo("202601011200001ABC");
        order.setTotalAmount(new BigDecimal("178.00"));
        order.setStatus("PAID");
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.singletonList(order));
        when(orderItemRepository.findByOrderId(1L))
                .thenReturn(Collections.emptyList());

        List<OrderDto> result = orderService.listByUser(1L);

        assertEquals(1, result.size());
        assertEquals("202601011200001ABC", result.get(0).getOrderNo());
    }

    @Test
    @DisplayName("个人购书统计 - 按时间范围聚合")
    void testPersonalStats() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setCreatedAt(LocalDateTime.now());

        OrderItem item1 = new OrderItem();
        item1.setTitle("三体");
        item1.setQuantity(2);
        item1.setSubtotal(new BigDecimal("178.00"));

        OrderItem item2 = new OrderItem();
        item2.setTitle("三体");
        item2.setQuantity(1);
        item2.setSubtotal(new BigDecimal("89.00"));

        when(orderRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                eq(1L), any(), any()))
                .thenReturn(Collections.singletonList(order));
        when(orderItemRepository.findByOrderId(1L))
                .thenReturn(Arrays.asList(item1, item2));

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        Map<String, Object> result = orderService.personalStats(1L, start, end);

        assertNotNull(result);
        assertEquals(3, result.get("totalCount"));
        assertEquals(new BigDecimal("267.00"), result.get("totalAmount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> books = (List<Map<String, Object>>) result.get("books");
        assertEquals(1, books.size());
        assertEquals("三体", books.get(0).get("title"));
        assertEquals(3, books.get(0).get("count"));
    }

    @Test
    @DisplayName("订单详情 - 订单不存在时抛出异常")
    void testGetDetailNotFound() {
        when(orderRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> orderService.getDetail(1L, 99L));
    }
}
