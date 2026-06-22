package com.fidelity.moneytransfer.config;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "3f8b2c9d4e5a6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z7a8b9c0d");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 600000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L);
        user = User.builder().username("john").password("pw")
                .authorities(Collections.emptyList()).build();
    }

    @Test
    void generateToken_andExtractAllClaims() {
        String token = jwtUtil.generateToken(user, "USER", 100L);
        assertEquals("john", jwtUtil.extractUsername(token));
        assertEquals("USER", jwtUtil.extractRole(token));
        assertEquals(100L, jwtUtil.extractAccountId(token));
        assertEquals("access", jwtUtil.extractTokenType(token));
        assertTrue(jwtUtil.extractExpiration(token).after(new Date()));
    }

    @Test
    void getAccessTokenExpiration_returnsConfiguredValue() {
        assertEquals(600000L, jwtUtil.getAccessTokenExpiration());
    }

    @Test
    void validateToken_validAccessToken_true() {
        String token = jwtUtil.generateToken(user, "USER", 1L);
        assertTrue(jwtUtil.validateToken(token, user));
    }

    @Test
    void validateToken_wrongUser_false() {
        String token = jwtUtil.generateToken(user, "USER", 1L);
        UserDetails other = User.builder().username("jane").password("pw")
                .authorities(Collections.emptyList()).build();
        assertFalse(jwtUtil.validateToken(token, other));
    }

    @Test
    void validateToken_refreshTokenRejectedByAccessValidation() {
        String refresh = jwtUtil.generateRefreshToken(user);
        // Wrong token type for an access validation.
        assertFalse(jwtUtil.validateToken(refresh, user));
    }

    @Test
    void validateRefreshToken_valid_true() {
        String refresh = jwtUtil.generateRefreshToken(user);
        assertEquals("refresh", jwtUtil.extractTokenType(refresh));
        assertTrue(jwtUtil.validateRefreshToken(refresh, user));
    }

    @Test
    void validateRefreshToken_accessTokenRejected() {
        String token = jwtUtil.generateToken(user, "USER", 1L);
        assertFalse(jwtUtil.validateRefreshToken(token, user));
    }

    @Test
    void validateRefreshToken_wrongUser_false() {
        String refresh = jwtUtil.generateRefreshToken(user);
        UserDetails other = User.builder().username("jane").password("pw")
                .authorities(Collections.emptyList()).build();
        assertFalse(jwtUtil.validateRefreshToken(refresh, other));
    }

    @Test
    void expiredToken_throwsOnParse() {
        // Negative TTL produces an already-expired token; parsing it throws.
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String expired = jwtUtil.generateToken(user, "USER", 1L);
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.extractUsername(expired));
    }
}
