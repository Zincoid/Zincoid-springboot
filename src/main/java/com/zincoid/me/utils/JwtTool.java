package com.zincoid.me.utils;

import com.zincoid.me.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTool {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32)
            throw new IllegalStateException("jwt.secret must be at least 32 bytes");
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generate(Long userId, Role role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role.getValue())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Object value = parse(token).get("userId");
        return value instanceof Number n ? n.longValue() : null;
    }

    public Role getRole(String token) {
        Object role = parse(token).get("role");
        if (!(role instanceof Number n)) return null;
        return Role.fromValue(n.intValue());
    }
}
