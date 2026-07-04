package com.bookstore.repository;

import com.bookstore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA 仓储接口 —— 持久化 {@link Order}。
 * <p>
 * 提供按用户、日期范围的组合查询，由 Spring Data 通过方法名自动派生 SQL。
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findAllByOrderByCreatedAtDesc();

    // 日期范围查询（用户维度）
    List<Order> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, LocalDateTime start, LocalDateTime end);

    List<Order> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime start);

    List<Order> findByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(Long userId, LocalDateTime end);

    // 日期范围查询（全局维度，管理员用）
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    List<Order> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime start);

    List<Order> findByCreatedAtBeforeOrderByCreatedAtDesc(LocalDateTime end);
}
