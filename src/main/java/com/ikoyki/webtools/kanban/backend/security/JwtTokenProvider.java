package com.ikoyki.webtools.kanban.backend.security;

import com.ikoyki.webtools.kanban.backend.entity.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long validityInMilliseconds;

    public JwtTokenProvider(
            @Value("${security.jwt.token.secret-key}") String secret,
            @Value("${security.jwt.token.expire-length}") long validityInMilliseconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
    }

    // This matches the exact method signature expected by your AuthService
    public String generateToken(UserEntity user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(user.getEmail())                  // useremail
                .setId(UUID.randomUUID().toString())          // token id
                .claim("uid", user.getId().toString())  // the user id
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }
}
