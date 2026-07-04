package com.bookstore.service.impl;

import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import com.bookstore.entity.User;
import com.bookstore.repository.OrderItemRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.StatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 统计业务实现。
 * <p>
 * 从订单和订单明细表聚合销量与消费数据，支持按时间范围过滤。
 */
@Service
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public StatisticsServiceImpl(OrderRepository orderRepository,
                                  OrderItemRepository orderItemRepository,
                                  UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Map<String, Object>> bookSalesRanking(LocalDate startDate, LocalDate endDate) {
        List<Order> orders = getFilteredOrders(startDate, endDate);
        // 按书名聚合销量
        Map<String, Map<String, Object>> salesMap = new LinkedHashMap<>();
        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                String title = item.getTitle();
                Map<String, Object> stat = salesMap.computeIfAbsent(title, k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("title", title);
                    m.put("quantity", 0);
                    m.put("amount", BigDecimal.ZERO);
                    return m;
                });
                stat.put("quantity", (int) stat.get("quantity") + item.getQuantity());
                stat.put("amount", ((BigDecimal) stat.get("amount")).add(item.getSubtotal()));
            }
        }
        // 按销量降序排序
        List<Map<String, Object>> result = new ArrayList<>(salesMap.values());
        result.sort((a, b) -> Integer.compare((int) b.get("quantity"), (int) a.get("quantity")));
        return result;
    }

    @Override
    public List<Map<String, Object>> userSpendingRanking(LocalDate startDate, LocalDate endDate) {
        List<Order> orders = getFilteredOrders(startDate, endDate);
        // 按用户ID聚合消费金额
        Map<Long, BigDecimal> spendingMap = new LinkedHashMap<>();
        for (Order order : orders) {
            spendingMap.merge(order.getUserId(), order.getTotalAmount(), BigDecimal::add);
        }
        // 查询用户名
        Map<Long, String> userNames = new HashMap<>();
        for (Long uid : spendingMap.keySet()) {
            userRepository.findById(uid).ifPresent(u -> userNames.put(uid, u.getUsername()));
        }
        // 构建结果并按金额降序排序
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : spendingMap.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", entry.getKey());
            m.put("username", userNames.getOrDefault(entry.getKey(), "未知用户"));
            m.put("totalAmount", entry.getValue());
            result.add(m);
        }
        result.sort((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")));
        return result;
    }

    private List<Order> getFilteredOrders(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        if (start != null && end != null) {
            return orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        } else if (start != null) {
            return orderRepository.findByCreatedAtAfterOrderByCreatedAtDesc(start);
        } else if (end != null) {
            return orderRepository.findByCreatedAtBeforeOrderByCreatedAtDesc(end);
        } else {
            return orderRepository.findAllByOrderByCreatedAtDesc();
        }
    }
}
