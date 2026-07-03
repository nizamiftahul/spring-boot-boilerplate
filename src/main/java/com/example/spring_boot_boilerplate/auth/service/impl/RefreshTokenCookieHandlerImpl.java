package com.example.spring_boot_boilerplate.auth.service.impl;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.example.spring_boot_boilerplate.auth.service.RefreshTokenCookieHandler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class RefreshTokenCookieHandlerImpl implements RefreshTokenCookieHandler {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCookieHandlerImpl.class);

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Duration refreshTokenExpiration;
    private final boolean isProduction;

    public RefreshTokenCookieHandlerImpl(
            @Value("${jwt.refresh-token.expiration-ms}") long refreshTokenExpirationMs,
            @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.isProduction = "prod".equalsIgnoreCase(activeProfile);
        this.refreshTokenExpiration = Duration.ofMillis(refreshTokenExpirationMs);
    }

    @Override
    public void write(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .sameSite(isProduction ? "Strict" : "Lax")
                .secure(isProduction) // HTTPS only in production
                .path("/")
                .maxAge(refreshTokenExpiration.getSeconds()) // 7 days
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        logger.debug("Refresh token cookie set");
    }

    @Override
    public void delete(HttpServletResponse response) {
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

    @Override
    public String extract(HttpServletRequest request) {
        String token = getFromCookie(request);
        return token != null ? token : getFromHeader(request);
    }

    private String getFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getFromHeader(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
