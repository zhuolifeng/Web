package com.bookstore.cloud;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final long ttlSeconds;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.issuer:ebookstore-cloud}") String issuer,
                      @Value("${jwt.ttl-seconds:3600}") long ttlSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        if (ttlSeconds < 1 || ttlSeconds > 86400) throw new IllegalArgumentException("JWT TTL must be 1–86400 seconds");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(String username) {
        Instant now = Instant.now();
        return Jwts.builder().issuer(issuer).subject(username).claim("role", "USER")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public Claims verify(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                .parseSignedClaims(token).getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getExpiration() == null || !"USER".equals(claims.get("role", String.class)))
            throw new JwtException("Required claims are missing or invalid");
        return claims;
    }

    public long ttlSeconds() { return ttlSeconds; }
}
