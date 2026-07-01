package com.example.spring_boot_boilerplate.auth;

import com.example.spring_boot_boilerplate.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Authentication Service.
 * 
 * Encapsulates business logic for token lifecycle management:
 * - Create access & refresh tokens
 * - Validate & rotate tokens
 * - Revoke tokens (logout)
 * 
 * Single Responsibility: Manage token operations and token store.
 */
@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Constructor injection of dependencies.
     */
    public AuthService(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Create a new refresh token for a user.
     * 
     * @param username the user
     * @param expiryDurationMs duration in milliseconds
     * @return refresh token value
     */
    public String createRefreshToken(String username, long expiryDurationMs) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken(
                token,
                username,
                Instant.now().plusMillis(expiryDurationMs)
        );
        refreshTokenRepository.save(refreshToken);
        logger.debug("Created refresh token for user: {}", username);
        return token;
    }

    /**
     * Validate and rotate refresh token.
     * Deletes old token, creates new one with same expiry offset.
     * 
     * @param oldRefreshToken the refresh token to validate
     * @return new refresh token, null if invalid or expired
     */
    @Transactional
    public String rotateRefreshToken(String oldRefreshToken, long expiryDurationMs) {
        var optToken = refreshTokenRepository.findByToken(oldRefreshToken);
        
        if (optToken.isEmpty()) {
            logger.warn("Refresh token not found: {}", oldRefreshToken);
            return null;
        }

        RefreshToken token = optToken.get();

        // Check expiry
        if (Instant.now().isAfter(token.getExpiryDate())) {
            logger.warn("Refresh token expired for user: {}", token.getUsername());
            refreshTokenRepository.delete(token);
            return null;
        }

        // Rotate: delete old, create new
        refreshTokenRepository.delete(token);
        return createRefreshToken(token.getUsername(), expiryDurationMs);
    }

    /**
     * Revoke a refresh token (logout).
     * 
     * @param refreshToken the token to revoke
     */
    public void revokeRefreshToken(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            refreshTokenRepository.delete(token);
            logger.debug("Revoked refresh token for user: {}", token.getUsername());
        });
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices).
     * 
     * @param username the user
     */
    @Transactional
    public void revokeAllUserTokens(String username) {
        refreshTokenRepository.deleteByUsername(username);
        logger.info("Revoked all refresh tokens for user: {}", username);
    }

    /**
     * Validate refresh token and return associated username.
     * 
     * @param refreshToken the token to validate
     * @return username if valid and not expired, null otherwise
     */
    public String validateRefreshToken(String refreshToken) {
        var optToken = refreshTokenRepository.findByToken(refreshToken);
        
        if (optToken.isEmpty()) {
            return null;
        }

        RefreshToken token = optToken.get();
        if (Instant.now().isAfter(token.getExpiryDate())) {
            refreshTokenRepository.delete(token);
            return null;
        }

        return token.getUsername();
    }
}
