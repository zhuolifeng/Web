package com.bookstore.repository;

import com.bookstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA 仓储接口 —— 持久化 {@link User}。
 * <p>
 * 继承 {@code JpaRepository<User, Long>} 即可获得标准 CRUD；
 * 下面的 {@code findByXxx / existsByXxx} 由 Spring Data 通过方法名自动派生
 * 出对应的 SQL，无需手写实现类。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** 按创建时间倒序查询全部用户（管理员后台） */
    List<User> findAllByOrderByCreatedAtDesc();
}
