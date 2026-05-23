package com.bookstore.service.impl;

import com.bookstore.dto.UserRegisterRequest;
import com.bookstore.entity.User;
import com.bookstore.exception.BusinessException;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(409, "用户名已存在");
        }
        if (request.getEmail() != null
                && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(409, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname());
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return user;
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, String nickname, String email, String phone) {
        User user = getById(userId);
        if (nickname != null) user.setNickname(nickname);
        if (email != null && !email.isBlank()) {
            userRepository.findByEmail(email).ifPresent(other -> {
                if (!other.getId().equals(userId)) {
                    throw new BusinessException(409, "邮箱已被注册");
                }
            });
            user.setEmail(email);
        }
        if (phone != null) user.setPhone(phone);
        return userRepository.save(user);
    }
}
