package com.bookstore.repository;

import com.bookstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    /** 按用户名查询用户 —— 登录、注册时校验。 */
    Optional<User> findByUsername(String username);

    /** 按邮箱查询用户 —— 注册 / 修改资料时校验。 */
    Optional<User> findByEmail(String email);

    /** 用户名是否已存在（注册查重）。 */
    boolean existsByUsername(String username);

    /** 邮箱是否已被注册（注册查重）。 */
    boolean existsByEmail(String email);
}
