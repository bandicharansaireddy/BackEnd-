package com.freshmart.api.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {
    private static final String ISSUER = "freshmart-test";
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[48]);

    @Test
    void validatesSignatureIssuerAndAccessClaims() {
        JwtTokenService tokens = new JwtTokenService(KEY, ISSUER, 15);
        String jwt = tokens.issue(42L, "CUSTOMER");
        var claims = tokens.parse(jwt);

        assertEquals("42", claims.getSubject());
        assertEquals(ISSUER, claims.getIssuer());
        assertEquals("CUSTOMER", claims.get("role", String.class));
        assertEquals("access", claims.get("purpose", String.class));
        assertEquals(900, tokens.accessLifetimeSeconds());
        assertThrows(RuntimeException.class, () -> new JwtTokenService(
            Base64.getEncoder().encodeToString(new byte[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48}), ISSUER, 15).parse(jwt));
    }

    @Test
    void rejectsExpiredTokens() {
        byte[] secret = Base64.getDecoder().decode(KEY);
        var signingKey = Keys.hmacShaKeyFor(secret);
        String expired = Jwts.builder().issuer(ISSUER).subject("42")
            .issuedAt(Date.from(Instant.now().minusSeconds(30)))
            .expiration(Date.from(Instant.now().minusSeconds(1))).signWith(signingKey).compact();
        JwtTokenService tokens = new JwtTokenService(KEY, ISSUER, 15);
        assertThrows(ExpiredJwtException.class, () -> tokens.parse(expired));
    }
}
