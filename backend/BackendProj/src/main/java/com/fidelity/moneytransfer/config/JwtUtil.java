package com.fidelity.moneytransfer.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ACCOUNT_ID = "accountId";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private Long expiration;

    @Value("${app.jwt.refresh-expiration}")
    private Long refreshExpiration;

    // Generate short-lived access token for user
    public String generateToken(UserDetails userDetails,
                                String role,
                                Long accountId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_ACCOUNT_ID, accountId);
        claims.put(CLAIM_TOKEN_TYPE, TYPE_ACCESS);
        return createToken(claims, userDetails.getUsername(), expiration);
    }

    // Generate long-lived refresh token (only identifies the user)
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TYPE_REFRESH);
        return createToken(claims, userDetails.getUsername(), refreshExpiration);
    }

    // Validate an access token against the resolved user
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && TYPE_ACCESS.equals(extractTokenType(token))
                && !isTokenExpired(token);
    }

    // Validate a refresh token against the resolved user
    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && TYPE_REFRESH.equals(extractTokenType(token))
                && !isTokenExpired(token);
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    // Configured access-token lifetime in milliseconds
    public Long getAccessTokenExpiration() {
        return expiration;
    }

    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract role from token
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // Extract accountId from token
    public Long extractAccountId(String token) {
        return extractAllClaims(token).get("accountId", Long.class);
    }

    // Extract expiration date
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token,
                              Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private String createToken(Map<String, Object> claims,
                               String subject,
                               Long ttlMillis) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(
                        new Date(System.currentTimeMillis() + ttlMillis)
                )
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}