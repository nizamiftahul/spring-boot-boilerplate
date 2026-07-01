package com.example.spring_boot_boilerplate.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * JWT Token Provider component.
 * 
 * Responsible for token creation and validation. Uses Strategy pattern
 * to encapsulate JWT-specific operations (HS256 HMAC signing).
 * 
 * Single Responsibility: Manage JWT access token lifecycle.
 */
@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String ROLES_CLAIM = "roles";

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * Constructor injection of JWT configuration from application.yml.
     * Uses SecretKey for secure HMAC signing (HS256).
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
        logger.debug("JwtTokenProvider initialized with {} ms expiration", expirationMs);
    }

    /**
     * Create a new JWT access token with username and roles.
     * 
     * @param username the subject (username)
     * @param roles the list of user roles
     * @return JWT token string
     */
    public String createAccessToken(String username, List<String> roles) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationMs);

            String token = Jwts.builder()
                    .setSubject(username)
                    .claim(ROLES_CLAIM, roles)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(secretKey, SignatureAlgorithm.HS256)
                    .compact();

            logger.debug("JWT access token created for user: {}", username);
            return token;
        } catch (Exception e) {
            logger.error("Failed to create JWT access token for user: {}", username, e);
            throw new JwtException("Token creation failed", e);
        }
    }

    /**
     * Validate JWT token signature and expiration.
     * 
     * Strategy: Fail-safe validation - returns false on any error (signature,
     * expiration, malformed). Does not throw exception.
     * 
     * @param token the JWT token string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            logger.debug("JWT token validation successful");
            return true;
        } catch (JwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token is empty or null", e);
            return false;
        }
    }

    /**
     * Extract username (subject) from token.
     * 
     * @param token the JWT token string
     * @return username or null if extraction fails
     */
    public String getUsername(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            logger.warn("Failed to extract username from token", e);
            return null;
        }
    }

    /**
     * Extract roles from token claims.
     * 
     * @param token the JWT token string
     * @return list of roles or empty list if extraction fails
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            List<String> roles = claims.get(ROLES_CLAIM, List.class);
            return roles != null ? roles : List.of();
        } catch (Exception e) {
            logger.warn("Failed to extract roles from token", e);
            return List.of();
        }
    }

    /**
     * Parse and extract claims from token without validation.
     * Used internally for claim extraction after validation.
     * 
     * @param token the JWT token string
     * @return Claims object
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
