package com.hubnex.backend.service;

import com.hubnex.backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final RoleAccessService roleAccessService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(user.getLogin())
                .claim("role", resolveRoleName(user))
                .claim("authorities", roleAccessService.resolveAuthorities(user))
                .claim("permissions", user.getRoleEntity() != null
                        ? roleAccessService.resolveFinalPermissions(user.getRoleEntity())
                        : java.util.Set.of())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String extractLogin(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, User user) {
        return user.getLogin().equals(extractLogin(token)) && extractExpiration(token).after(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String resolveRoleName(User user) {
        if (user.getRoleEntity() != null && user.getRoleEntity().getName() != null) {
            return user.getRoleEntity().getName();
        }
        return user.getRole() != null ? user.getRole().name() : null;
    }
}
