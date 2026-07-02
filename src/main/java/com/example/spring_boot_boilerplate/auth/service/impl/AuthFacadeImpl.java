package com.example.spring_boot_boilerplate.auth.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.spring_boot_boilerplate.auth.dto.AuthResponse;
import com.example.spring_boot_boilerplate.auth.dto.LoginRequest;
import com.example.spring_boot_boilerplate.auth.service.AuthFacade;
import com.example.spring_boot_boilerplate.auth.service.RefreshTokenService;
import com.example.spring_boot_boilerplate.common.exception.UnauthorizedException;
import com.example.spring_boot_boilerplate.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthFacadeImpl implements AuthFacade {
    private static final Logger logger = LoggerFactory.getLogger(AuthFacadeImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthFacadeImpl(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public AuthResponse login(
            LoginRequest request, HttpServletResponse response) {
        logger.info("Login attempt for user: {}", request.getUsername());

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
        boolean limitEnforced = refreshTokenService.enforceConcurrentLimit(request.getUsername());
        if (limitEnforced) {
            logger.info("Concurrent token limit enforced for user: {}",
                    request.getUsername());
        }

        // Create and store refresh token
        String refreshToken = refreshTokenService.create(
                request.getUsername());

        // Set refresh token in HttpOnly cookie
        refreshTokenService.setCookie(response, refreshToken);

        logger.info("User logged in: {} (concurrent limit: {})",
                request.getUsername(), limitEnforced);

        return (new AuthResponse(accessToken,
                request.getUsername()));
    }

    public AuthResponse refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("Token refresh requested");

        // Get refresh token: cookie first, then header
        String refreshToken = refreshTokenService.getFromCookie(request);
        if (refreshToken == null) {
            refreshToken = refreshTokenService.getFromHeader(request);
        }

        if (refreshToken == null) {
            logger.warn("No refresh token found in cookie or header");
            throw new UnauthorizedException("Unauthorized: No refresh token provided");
        }

        // Validate refresh token and get username
        String username = refreshTokenService.validate(refreshToken);
        if (username == null) {
            logger.warn("Invalid or expired refresh token");
            throw new UnauthorizedException("Unauthorized: Invalid or expired refresh token");
        }

        // Get user roles for new access token
        // (In a real app, fetch from UserDetailsService or database)
        List<String> roles = List.of("USER");

        // Create new access token
        String newAccessToken = jwtTokenProvider.createAccessToken(username, roles);

        // Rotate refresh token
        String newRefreshToken = refreshTokenService.rotate(
                refreshToken);

        if (newRefreshToken == null) {
            logger.warn("Failed to rotate refresh token for user: {}", username);
            throw new UnauthorizedException("Unauthorized: Failed to rotate refresh token for user: " + username);
        }

        // Set new refresh token cookie
        refreshTokenService.setCookie(response, newRefreshToken);

        logger.debug("Token refreshed for user: {}", username);
        return (new AuthResponse(newAccessToken, username));

    }

    public void logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("Logout requested");

        // Get refresh token
        String refreshToken = refreshTokenService.getFromCookie(request);
        if (refreshToken == null) {
            refreshToken = refreshTokenService.getFromHeader(request);
        }

        // Revoke if found
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }

        // Clear cookie
        refreshTokenService.clearCookie(response);

        logger.info("User logged out");
    }
}
