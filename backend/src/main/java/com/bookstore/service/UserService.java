package com.bookstore.service;

import com.bookstore.dto.UserDto;
import com.bookstore.dto.UserRegisterRequest;

/**
 * 用户业务接口。
 * <p>
 * 全部返回 {@link UserDto}，不暴露 JPA {@code User} Entity，
 * 也不会把 password 字段泄漏给上层。Controller 只与 DTO 通信，
 * 真实存储（当前 MySQL via Spring Data JPA）可在不影响 API 的前提下替换。
 */
public interface UserService {

    /** 注册新用户。对应 POST /api/v1/users/register */
    UserDto register(UserRegisterRequest request);

    /** 校验用户名 + 密码，成功返回 UserDto。 */
    UserDto login(String username, String password);

    /** 按 id 查询用户。 */
    UserDto getById(Long id);

    /** 更新昵称 / 邮箱 / 手机号。 */
    UserDto updateProfile(Long userId, String nickname, String email, String phone);
}
