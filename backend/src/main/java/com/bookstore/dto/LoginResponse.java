package com.bookstore.dto;

public class LoginResponse {

    private String token;
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String role;

    public LoginResponse() {}

    public LoginResponse(String token, Long id, String username,
                         String email, String nickname, String role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
