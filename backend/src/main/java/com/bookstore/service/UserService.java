package com.bookstore.service;

import com.bookstore.dto.UserRegisterRequest;
import com.bookstore.entity.User;

/**
 * 用户业务接口。Controller 仅依赖该接口，实现由 {@code service.impl.UserServiceImpl} 提供，
 * 体现"接口与实现分离"原则。
 */
public interface UserService {

    User register(UserRegisterRequest request);

    User login(String username, String password);

    User getById(Long id);

    User updateProfile(Long userId, String nickname, String email, String phone);
}
