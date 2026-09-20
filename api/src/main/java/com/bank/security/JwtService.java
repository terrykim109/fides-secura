package com.bank.security;

import com.bank.config.AppProperties;
import com.bank.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final AppProperties appProperties;
    private final SecretKey key;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        byte[] secretBytes = appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String createAccessToken(Long userId, String email, Role role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(appProperties.jwt().accessTokenMinutes() * 60);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
