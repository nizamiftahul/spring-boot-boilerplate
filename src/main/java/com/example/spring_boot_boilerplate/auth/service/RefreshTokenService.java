package com.example.spring_boot_boilerplate.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

public interface RefreshTokenService {
    /**
     * Create a new refresh token for a user.
     *
     * @param username         the user
     * @param expiryDurationMs duration in milliseconds
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
     * @param oldRefreshToken  the refresh token to validate
     * @param expiryDurationMs duration in milliseconds
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
    void revokeAllUserTokens(String username);

    /**
     * Enforce concurrent token limit by revoking oldest token if limit exceeded.
     *
     * @param username User identifier
     * @return true if limit enforced (oldest token revoked), false if under limit
     */
    @Transactional
    public boolean enforceConcurrentLimit(String username);

    public int getMaxConcurrentTokens();

    /**
     * Set the refresh token in the HTTP response as a cookie.
     *
     * @param response     the HTTP response
     * @param refreshToken the refresh token to set
     */
    void setCookie(HttpServletResponse response, String refreshToken);

    /**
     * Clear the refresh token cookie in the HTTP response.
     *
     * @param response the HTTP response
     */
    void clearCookie(HttpServletResponse response);

    /**
     * Get the refresh token from the HTTP request cookie.
     *
     * @param request the HTTP request
     * @return the refresh token if present, null otherwise
     */
    String getFromCookie(HttpServletRequest request);

    /**
     * Get the refresh token from the HTTP request header.
     *
     * @param request the HTTP request
     * @return the refresh token if present, null otherwise
     */
    String getFromHeader(HttpServletRequest request);
}
