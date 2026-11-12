package com.bookstore.cloud;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Explicit demo credentials, independent of the original bookstore's user database. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Credentials(@NotBlank @Size(max = 64) String username,
                              @NotBlank @Size(max = 128) String password) {}
    public record AccessToken(String accessToken, String tokenType, long expiresIn) {}
    private final String username;
    private final String passwordHash;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(@Value("${bookstore.demo.username}") String username,
                          @Value("${bookstore.demo.password}") String password,
                          PasswordEncoder encoder, JwtService jwt) {
        this.username = username;
        this.passwordHash = encoder.encode(password);
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<AccessToken>> token(@Valid @RequestBody Credentials credentials) {
        boolean correctPassword = encoder.matches(credentials.password(), passwordHash);
        if (!correctPassword || !username.equals(credentials.username()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(new AccessToken(jwt.issue(username), "Bearer", jwt.ttlSeconds())));
    }
}
