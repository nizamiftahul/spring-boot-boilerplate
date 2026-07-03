package com.example.spring_boot_boilerplate.auth.service;

public interface RefreshTokenService {
    /**
     * Create a new refresh token for a user.
     *
     * @param username the user
     * @return refresh token value
     */
    String create(String username);

    /**
     * Validate refresh token.
     *
     * @param refreshToken the refresh token to validate
     * @return the username if valid, null if invalid
     */
    String validate(String refreshToken);

    /**
     * Validate and rotate refresh token.
     *
     * @param oldRefreshToken the refresh token to validate
     * @return new refresh token value if valid, null if invalid
     */
    String rotate(String oldRefreshToken);

    /**
     * Revoke a refresh token (logout).
     *
     * @param refreshToken the refresh token to revoke
     */
    void revoke(String refreshToken);

    /**
     * Revoke all refresh tokens for a user (logout).
     *
     * @param username the user whose refresh tokens to revoke
     */
    void revokeAll(String username);

    /**
     * Enforce token limit by revoking oldest token if limit exceeded.
     *
     * @param username User identifier
     * @return true if limit enforced (oldest token revoked), false if under limit
     */
    boolean enforceTokenLimit(String username);

    /**
     * Get the maximum number of concurrent refresh tokens allowed per user.
     *
     * @return max concurrent tokens
     */
    int getMaxConcurrentTokens();
}
