package com.school.library.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expireDays;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-days}") long expireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireDays = expireDays;
    }

    public String generate(AppPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.account())
                .claim("uid", principal.userId())
                .claim("role", principal.role().name())
                .claim("rtype", principal.readerType())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireDays * 24 * 3600)))
                .signWith(key)
                .compact();
    }

    /** 解析并校验 token，无效或过期抛 JwtException */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
