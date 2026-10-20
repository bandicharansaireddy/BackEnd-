package com.freshmart.api.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenService {
    private final SecretKey key;
    private final String issuer;
    private final long accessMinutes;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
                           @Value("${app.jwt.issuer}") String issuer,
                           @Value("${app.jwt.access-minutes}") long accessMinutes) {
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length < 32) throw new IllegalArgumentException("JWT secret must contain at least 256 bits");
            this.key = Keys.hmacShaKeyFor(decoded);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Configure JWT_SECRET with a base64-encoded key of at least 32 bytes", e);
        }
        if (accessMinutes < 1 || accessMinutes > 60) throw new IllegalStateException("JWT access lifetime must be 1 to 60 minutes");
        this.issuer = issuer;
        this.accessMinutes = accessMinutes;
    }

    public String issue(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder().issuer(issuer).subject(userId.toString()).claim("role", role).claim("purpose", "access")
            .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(accessMinutes * 60)))
            .signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer(issuer).build().parseSignedClaims(token).getPayload();
    }

    public String issueReset(Long userId, String otpId) {
        Instant now = Instant.now();
        return Jwts.builder().issuer(issuer).subject(userId.toString()).claim("purpose", "password_reset")
            .claim("otpId", otpId).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(300)))
            .signWith(key).compact();
    }

    public long accessLifetimeSeconds() { return accessMinutes * 60; }
}
