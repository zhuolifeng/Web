package com.bookstore.dto;

import com.bookstore.entity.User;

import java.time.LocalDateTime;

/**
 * User 数据传输对象。
 *
 * 作用：
 *   1) 屏蔽底层 Entity（com.bookstore.entity.User）以及持久化实现（当前 JPA / MySQL，
 *      下学期可能是 MongoDB / Redis），让 Controller 与前端只面对稳定的 DTO 契约。
 *   2) 安全：默认 <b>不</b> 暴露 password 字段，避免泄漏 BCrypt 密文。
 */
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;

    public UserDto() {}

    /** 把 JPA Entity 安全转成 DTO（剔除 password）。 */
    public static UserDto from(User user) {
        if (user == null) return null;
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.phone = user.getPhone();
        dto.nickname = user.getNickname();
        dto.role = user.getRole();
        dto.createdAt = user.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
