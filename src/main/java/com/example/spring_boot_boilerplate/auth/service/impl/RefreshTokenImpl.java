package com.example.spring_boot_boilerplate.auth.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.example.spring_boot_boilerplate.auth.service.RefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.spring_boot_boilerplate.auth.entity.RefreshToken;
import com.example.spring_boot_boilerplate.auth.repository.RefreshTokenRepository;

@Service
public class RefreshTokenImpl implements RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenImpl.class);

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Duration refreshTokenExpirationMs;
    private final RefreshTokenRepository refreshTokenRepository;
    private final boolean isProduction;
    private final int maxConcurrentTokens;

    public RefreshTokenImpl(RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token.expiration-ms}") long refreshTokenExpirationMs,
            @Value("${spring.profiles.active:dev}") String activeProfile,
            @Value("${jwt.max-concurrent-tokens:5}") int maxConcurrentTokens) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = Duration.ofMillis(refreshTokenExpirationMs);
        this.isProduction = "prod".equalsIgnoreCase(activeProfile);
        this.maxConcurrentTokens = maxConcurrentTokens;
    }

    @Override
    public String create(String username) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken(
                token,
                username,
                Instant.now().plus(refreshTokenExpirationMs));
        refreshTokenRepository.save(refreshToken);
        logger.debug("Created refresh token for user: {}", username);
        return token;
    }

    @Override
    public String rotate(String oldRefreshToken) {
        String username = validate(oldRefreshToken); // Validate first

        if (username == null) {
            return null;
        }

        // Rotate: delete old, create new
        refreshTokenRepository.findByToken(oldRefreshToken).ifPresent(refreshTokenRepository::delete);
        return create(username);
    }

    @Override
    public void revoke(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            refreshTokenRepository.delete(token);
            logger.debug("Revoked refresh token for user: {}", token.getUsername());
        });
    }

    @Override
    public void revokeAllUserTokens(String username) {
        refreshTokenRepository.deleteByUsername(username);
        logger.info("Revoked all refresh tokens for user: {}", username);
    }

    @Override
    public String validate(String refreshToken) {
        var optToken = refreshTokenRepository.findByToken(refreshToken);

        if (optToken.isEmpty()) {
            logger.warn("Refresh token not found: {}", refreshToken);
            return null;
        }

        RefreshToken token = optToken.get();
        if (Instant.now().isAfter(token.getExpiresAt())) {
            logger.warn("Refresh token expired for user: {}", token.getUsername());
            refreshTokenRepository.delete(token);
            return null;
        }

        return token.getUsername();
    }

    @Override
    public void setCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .sameSite(isProduction ? "Strict" : "Lax")
                .secure(isProduction) // HTTPS only in production
                .path("/")
                .maxAge(refreshTokenExpirationMs.getSeconds()) // 7 days
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        logger.debug("Refresh token cookie set");
    }

    @Override
    public void clearCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .sameSite(isProduction ? "Strict" : "Lax")
                .secure(isProduction)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        logger.debug("Refresh token cookie cleared");
    }

    /**
     * Enforce concurrent token limit by revoking oldest token if limit exceeded.
     *
     * @param username User identifier
     * @return true if limit enforced (oldest token revoked), false if under limit
     */
    @Override
    public boolean enforceConcurrentLimit(String username) {
        List<RefreshToken> userTokens = (List<RefreshToken>) refreshTokenRepository.findByUsername(username);

        if (userTokens.size() < maxConcurrentTokens) {
            return false; // Under limit, no action needed
        }

        // Revoke oldest token
        RefreshToken oldestToken = userTokens.stream()
                .min((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()))
                .orElse(null);

        if (oldestToken != null) {
            refreshTokenRepository.delete(oldestToken);
            logger.warn("Revoked oldest refresh token for user {} to enforce concurrent limit of {}",
                    username, maxConcurrentTokens);
            return true;
        }

        return false;
    }

    @Override
    public int getMaxConcurrentTokens() {
        return maxConcurrentTokens;
    }

    @Override
    public String getFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    public String getFromHeader(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
