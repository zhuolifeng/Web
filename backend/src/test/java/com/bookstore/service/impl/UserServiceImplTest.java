package com.bookstore.service.impl;

import com.bookstore.dto.UserDto;
import com.bookstore.dto.UserRegisterRequest;
import com.bookstore.entity.User;
import com.bookstore.exception.BusinessException;
import com.bookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试。
 * 覆盖注册、登录、禁用/解禁等核心业务逻辑。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setPassword("$2a$10$encoded");
        sampleUser.setEmail("test@example.com");
        sampleUser.setRole("USER");
        sampleUser.setEnabled(true);
        sampleUser.setCreatedAt(LocalDateTime.now());
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册 - 成功注册新用户")
    void testRegisterSuccess() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("new@example.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserDto result = userService.register(request);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("USER", result.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("注册 - 用户名已存在时抛出异常")
    void testRegisterDuplicateUsername() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(request));
        assertEquals(409, ex.getCode());
        assertTrue(ex.getMessage().contains("用户名已存在"));
    }

    @Test
    @DisplayName("注册 - 邮箱已存在时抛出异常")
    void testRegisterDuplicateEmail() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(request));
        assertEquals(409, ex.getCode());
        assertTrue(ex.getMessage().contains("邮箱已被注册"));
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录 - 成功登录")
    void testLoginSuccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "$2a$10$encoded")).thenReturn(true);

        UserDto result = userService.login("testuser", "password123");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("登录 - 用户名不存在时抛出异常")
    void testLoginUsernameNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("nobody", "password"));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("登录 - 密码错误时抛出异常")
    void testLoginWrongPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong", "$2a$10$encoded")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("testuser", "wrong"));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("登录 - 被禁用用户登录时提示'您的账号已经被禁用'")
    void testLoginDisabledUser() {
        sampleUser.setEnabled(false);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "$2a$10$encoded")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("testuser", "password123"));
        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("您的账号已经被禁用"));
    }

    // ==================== 禁用/解禁测试 ====================

    @Test
    @DisplayName("禁用用户 - 成功禁用普通用户")
    void testToggleDisableUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserDto result = userService.toggleEnabled(1L, false);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("禁用用户 - 不能禁用管理员账号")
    void testToggleDisableAdmin() {
        sampleUser.setRole("ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.toggleEnabled(1L, false));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不能禁用管理员"));
    }

    @Test
    @DisplayName("解禁用户 - 成功解禁")
    void testToggleEnableUser() {
        sampleUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        userService.toggleEnabled(1L, true);

        verify(userRepository).save(any(User.class));
    }

    // ==================== 其他测试 ====================

    @Test
    @DisplayName("查询全部用户列表")
    void testListAll() {
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setRole("USER");
        user2.setEnabled(true);

        when(userRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList(sampleUser, user2));

        List<UserDto> result = userService.listAll();

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getById - 用户不存在时抛出异常")
    void testGetByIdNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> userService.getById(99L));
    }
}
