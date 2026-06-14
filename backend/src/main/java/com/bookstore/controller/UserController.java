package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.dto.LoginResponse;
import com.bookstore.dto.UserDto;
import com.bookstore.dto.UserLoginRequest;
import com.bookstore.dto.UserRegisterRequest;
import com.bookstore.exception.BusinessException;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.AuditService;
import com.bookstore.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户相关 REST 接口。
 * <p>
 * Controller 只依赖 {@link UserService} 返回的 {@link UserDto}，
 * 完全不接触 JPA Entity；这样下学期切换到其他存储时只需修改 service.impl，
 * Controller 与前端契约保持不变。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public UserController(UserService userService, JwtUtil jwtUtil, AuditService auditService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    /** POST /api/v1/users/register —— 用户注册 */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(
            @Valid @RequestBody UserRegisterRequest request,
            HttpServletRequest httpRequest) {
        UserDto saved = userService.register(request);
        auditService.log(saved.getId(), saved.getUsername(), "REGISTER", "新用户注册", httpRequest);
        LoginResponse resp = new LoginResponse(
                null, saved.getId(), saved.getUsername(),
                saved.getEmail(), saved.getNickname(), saved.getRole());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("注册成功", resp));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            UserDto user = userService.login(request.getUsername(), request.getPassword());
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            LoginResponse resp = new LoginResponse(
                    token, user.getId(), user.getUsername(),
                    user.getEmail(), user.getNickname(), user.getRole());
            auditService.log(user.getId(), user.getUsername(), "LOGIN_SUCCESS", "用户登录成功", httpRequest);
            return ResponseEntity.ok(ApiResponse.ok("登录成功", resp));
        } catch (BusinessException e) {
            auditService.log(null, request.getUsername(), "LOGIN_FAIL", "登录失败: " + e.getMessage(), httpRequest);
            throw e;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserDto user = userService.getById(userId);
        LoginResponse resp = new LoginResponse(
                null, user.getId(), user.getUsername(),
                user.getEmail(), user.getNickname(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<LoginResponse>> updateProfile(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDto user = userService.updateProfile(
                userId,
                body.get("nickname"),
                body.get("email"),
                body.get("phone"));
        LoginResponse resp = new LoginResponse(
                null, user.getId(), user.getUsername(),
                user.getEmail(), user.getNickname(), user.getRole());
        auditService.log(userId, username, "PROFILE_UPDATE", "更新个人信息", request);
        return ResponseEntity.ok(ApiResponse.ok("保存成功", resp));
    }
}
