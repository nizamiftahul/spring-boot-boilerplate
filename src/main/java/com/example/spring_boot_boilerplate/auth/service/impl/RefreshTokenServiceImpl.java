package com.example.spring_boot_boilerplate.auth.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.spring_boot_boilerplate.auth.service.RefreshTokenService;

import com.example.spring_boot_boilerplate.auth.entity.RefreshToken;
import com.example.spring_boot_boilerplate.auth.repository.RefreshTokenRepository;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    private final Duration refreshTokenExpirationMs;
    private final RefreshTokenRepository refreshTokenRepository;
    private final int maxConcurrentTokens;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token.expiration-ms}") long refreshTokenExpirationMs,
            @Value("${jwt.max-concurrent-tokens:5}") int maxConcurrentTokens) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = Duration.ofMillis(refreshTokenExpirationMs);
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
    public void revokeAll(String username) {
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
    public boolean enforceTokenLimit(String username) {
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
}
