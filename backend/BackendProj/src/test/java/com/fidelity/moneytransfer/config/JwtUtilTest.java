package com.fidelity.moneytransfer.config;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    private static final String SECRET =
            "3f8b2c9d4e5a6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z7a8b9c0d";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 600000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L);

        userDetails = User.builder()
                .username("john")
                .password("pw")
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    void generateAndValidateAccessToken() {
        String token = jwtUtil.generateToken(userDetails, "USER", 42L);

        assertTrue(jwtUtil.validateToken(token, userDetails));
        assertEquals("john", jwtUtil.extractUsername(token));
        assertEquals("USER", jwtUtil.extractRole(token));
        assertEquals(42L, jwtUtil.extractAccountId(token));
        assertEquals("access", jwtUtil.extractTokenType(token));
        assertEquals(600000L, jwtUtil.getAccessTokenExpiration());
        assertNotNull(jwtUtil.extractExpiration(token));
        // An access token is NOT a valid refresh token
        assertFalse(jwtUtil.validateRefreshToken(token, userDetails));
    }

    @Test
    void generateAndValidateRefreshToken() {
        String refresh = jwtUtil.generateRefreshToken(userDetails);

        assertTrue(jwtUtil.validateRefreshToken(refresh, userDetails));
        assertEquals("refresh", jwtUtil.extractTokenType(refresh));
        // A refresh token is NOT a valid access token
        assertFalse(jwtUtil.validateToken(refresh, userDetails));
    }

    @Test
    void validateToken_WrongUser_ReturnsFalse() {
        String token = jwtUtil.generateToken(userDetails, "USER", 1L);
        UserDetails other = User.builder()
                .username("someoneelse").password("x")
                .authorities(Collections.emptyList()).build();

        assertFalse(jwtUtil.validateToken(token, other));
    }

    @Test
    void validateRefreshToken_WrongUser_ReturnsFalse() {
        String refresh = jwtUtil.generateRefreshToken(userDetails);
        UserDetails other = User.builder().username("other").password("x")
                .authorities(Collections.emptyList()).build();
        assertFalse(jwtUtil.validateRefreshToken(refresh, other));
    }

    @Test
    void expiredToken_ThrowsOnValidation() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String expired = jwtUtil.generateToken(userDetails, "USER", 1L);

        assertThrows(ExpiredJwtException.class,
                () -> jwtUtil.validateToken(expired, userDetails));
    }
}
