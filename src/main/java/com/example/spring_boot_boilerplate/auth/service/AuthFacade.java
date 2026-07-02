package com.example.spring_boot_boilerplate.auth.service;

import com.example.spring_boot_boilerplate.auth.dto.AuthResponse;
import com.example.spring_boot_boilerplate.auth.dto.LoginRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthFacade {

    /**
     * Authenticate user, create access & refresh tokens.
     * Enforces concurrent token limit (revokes oldest if exceeded).
     * Returns access token in response body, refresh token in HttpOnly cookie.
     */
    public AuthResponse login(
            LoginRequest request, HttpServletResponse response);

    /**
     * Exchange refresh token for new access token.
     * Reads refresh token from HttpOnly cookie first, falls back to Authorization
     * header.
     * Rotates the refresh token (old revoked, new issued).
     */
    public AuthResponse refresh(
            HttpServletRequest request,
            HttpServletResponse response);

    /**
     * POST /auth/logout
     * Revoke refresh token (invalidate session).
     * Clears HttpOnly cookie.
     */
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response);
}
