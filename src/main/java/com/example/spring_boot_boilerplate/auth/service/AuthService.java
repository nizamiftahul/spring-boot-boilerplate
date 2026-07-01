package com.example.spring_boot_boilerplate.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public interface AuthService {

    /**
     * Create a new refresh token for a user.
     * 
     * @param username         the user
     * @param expiryDurationMs duration in milliseconds
     * @return refresh token value
     */
    public String createRefreshToken(String username, long expiryDurationMs);

    /**
     * Validate and rotate refresh token.
     * Deletes old token, creates new one with same expiry offset.
     * 
     * @param oldRefreshToken the refresh token to validate
     * @return new refresh token, null if invalid or expired
     */
    @Transactional
    public String rotateRefreshToken(String oldRefreshToken, long expiryDurationMs);

    /**
     * Revoke a refresh token (logout).
     * 
     * @param refreshToken the token to revoke
     */
    public void revokeRefreshToken(String refreshToken);

    /**
     * Revoke all refresh tokens for a user (logout from all devices).
     * 
     * @param username the user
     */
    @Transactional
    public void revokeAllUserTokens(String username);

    /**
     * Validate refresh token and return associated username.
     * 
     * @param refreshToken the token to validate
     * @return username if valid and not expired, null otherwise
     */
    public String validateRefreshToken(String refreshToken);
}
