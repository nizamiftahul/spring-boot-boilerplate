package com.example.spring_boot_boilerplate.auth.controller;

import com.example.spring_boot_boilerplate.auth.dto.AuthResponse;
import com.example.spring_boot_boilerplate.auth.dto.LoginRequest;
import com.example.spring_boot_boilerplate.auth.service.AuthFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller.
 * 
 * REST endpoints for login, token refresh, and logout.
 * Handles cookie-based refresh token storage and validation.
 * Enforces concurrent token limits per user.
 * 
 * Single Responsibility: HTTP request handling for auth operations.
 * Dependency Inversion: Depends on AuthService, ConcurrentTokenLimitService
 * abstractions.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthFacade authFacade;
    private final boolean isProduction;

    /**
     * Constructor injection of all dependencies.
     * Configuration from application.yml.
     */
    public AuthController(
            AuthFacade authFacade,
            @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.authFacade = authFacade;
        this.isProduction = "prod".equalsIgnoreCase(activeProfile);
        logger.info("AuthController initialized (prod: {})", isProduction);
    }

    /**
     * POST /auth/login
     * Authenticate user, create access & refresh tokens.
     * Enforces concurrent token limit (revokes oldest if exceeded).
     * Returns access token in response body, refresh token in HttpOnly cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authFacade.login(request, response);
        return ResponseEntity.ok(authResponse);

    }

    /**
     * POST /auth/refresh
     * Exchange refresh token for new access token.
     * Reads refresh token from HttpOnly cookie first, falls back to Authorization
     * header.
     * Rotates the refresh token (old revoked, new issued).
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authFacade.refresh(request, response);
        return ResponseEntity.ok(authResponse);

    }

    /**
     * POST /auth/logout
     * Revoke refresh token (invalidate session).
     * Clears HttpOnly cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authFacade.logout(request, response);
        return ResponseEntity.ok().build();
    }
}
