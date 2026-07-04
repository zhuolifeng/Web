package com.bookstore.service;

import com.bookstore.dto.UserDto;
import com.bookstore.dto.UserRegisterRequest;

import java.util.List;

/**
 * 用户业务接口。
 * <p>
 * 全部返回 {@link UserDto}，不暴露 JPA {@code User} Entity，
 * 也不会把 password 字段泄漏给上层。Controller 只与 DTO 通信，
 * 真实存储（当前 MySQL via Spring Data JPA）可在不影响 API 的前提下替换。
 */
public interface UserService {

    /** 注册新用户 */
    UserDto register(UserRegisterRequest request);

    /** 校验用户名 + 密码，检查是否被禁用，成功返回 UserDto */
    UserDto login(String username, String password);

    /** 按 id 查询用户 */
    UserDto getById(Long id);

    /** 更新昵称 / 邮箱 / 手机号 */
    UserDto updateProfile(Long userId, String nickname, String email, String phone);

    /** 查询全部用户列表（管理员用） */
    List<UserDto> listAll();

    /** 禁用或解禁用户（管理员用） */
    UserDto toggleEnabled(Long userId, boolean enabled);
}
