package com.example.spring_boot_boilerplate.auth.controller;

import com.example.spring_boot_boilerplate.auth.dto.AuthResponse;
import com.example.spring_boot_boilerplate.auth.dto.LoginRequest;
import com.example.spring_boot_boilerplate.auth.service.AuthService;
import com.example.spring_boot_boilerplate.auth.service.ConcurrentTokenLimitService;
import com.example.spring_boot_boilerplate.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final ConcurrentTokenLimitService concurrentTokenLimitService;
    private final long refreshTokenExpirationMs;
    private final boolean isProduction;

    /**
     * Constructor injection of all dependencies.
     * Configuration from application.yml.
     */
    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            AuthService authService,
            ConcurrentTokenLimitService concurrentTokenLimitService,
            @Value("${jwt.expiration-ms}") long jwtExpirationMs,
            @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authService = authService;
        this.concurrentTokenLimitService = concurrentTokenLimitService;
        // Refresh token expiration: 7 days (in milliseconds)
        this.refreshTokenExpirationMs = 7 * 24 * 60 * 60 * 1000L;
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
        try {
            logger.debug("Login attempt for user: {}", request.getUsername());

            // Authenticate
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            // Extract roles from authentication
            List<String> roles = auth.getAuthorities().stream()
                    .map(ga -> ga.getAuthority().replace("ROLE_", ""))
                    .toList();

            // Create access token
            String accessToken = jwtTokenProvider.createAccessToken(
                    request.getUsername(),
                    roles);

            // Enforce concurrent token limit (revoke oldest if needed)
            boolean limitEnforced = concurrentTokenLimitService.enforceConcurrentLimit(request.getUsername());
            if (limitEnforced) {
                logger.info("Concurrent token limit enforced for user: {}", request.getUsername());
            }

            // Create and store refresh token
            String refreshToken = authService.createRefreshToken(
                    request.getUsername(),
                    refreshTokenExpirationMs);

            // Set refresh token in HttpOnly cookie
            setRefreshTokenCookie(response, refreshToken);

            logger.info("User logged in: {} (concurrent limit: {})", request.getUsername(), limitEnforced);
            return ResponseEntity.ok(new AuthResponse(accessToken, request.getUsername()));

        } catch (AuthenticationException e) {
            logger.warn("Login failed for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
        try {
            logger.debug("Token refresh requested");

            // Get refresh token: cookie first, then header
            String refreshToken = getRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                refreshToken = getRefreshTokenFromHeader(request);
            }

            if (refreshToken == null) {
                logger.warn("No refresh token found in cookie or header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Validate refresh token and get username
            String username = authService.validateRefreshToken(refreshToken);
            if (username == null) {
                logger.warn("Invalid or expired refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Get user roles for new access token
            // (In a real app, fetch from UserDetailsService or database)
            List<String> roles = List.of("USER");

            // Create new access token
            String newAccessToken = jwtTokenProvider.createAccessToken(username, roles);

            // Rotate refresh token
            String newRefreshToken = authService.rotateRefreshToken(
                    refreshToken,
                    refreshTokenExpirationMs);

            if (newRefreshToken == null) {
                logger.warn("Failed to rotate refresh token for user: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Set new refresh token cookie
            setRefreshTokenCookie(response, newRefreshToken);

            logger.debug("Token refreshed for user: {}", username);
            return ResponseEntity.ok(new AuthResponse(newAccessToken, username));

        } catch (Exception e) {
            logger.error("Error during token refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
        try {
            logger.debug("Logout requested");

            // Get refresh token
            String refreshToken = getRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                refreshToken = getRefreshTokenFromHeader(request);
            }

            // Revoke if found
            if (refreshToken != null) {
                authService.revokeRefreshToken(refreshToken);
            }

            // Clear cookie
            clearRefreshTokenCookie(response);

            logger.info("User logged out");
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper methods for cookie management

    /**
     * Extract refresh token from HttpOnly cookie.
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Extract refresh token from Authorization header.
     * Falls back if not in cookie.
     */
    private String getRefreshTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        return null;
    }

    /**
     * Set refresh token in secure HttpOnly cookie.
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction); // HTTPS only in production
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpirationMs / 1000)); // 7 days
        response.addCookie(cookie);
        logger.debug("Refresh token cookie set");
    }

    /**
     * Clear refresh token cookie (logout).
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        logger.debug("Refresh token cookie cleared");
    }
}
