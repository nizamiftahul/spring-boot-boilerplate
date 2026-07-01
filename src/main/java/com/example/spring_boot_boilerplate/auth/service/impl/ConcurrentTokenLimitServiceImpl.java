package com.example.spring_boot_boilerplate.auth.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_boilerplate.auth.entity.RefreshToken;
import com.example.spring_boot_boilerplate.auth.repository.RefreshTokenRepository;
import com.example.spring_boot_boilerplate.auth.service.ConcurrentTokenLimitService;

/**
 * Enforces concurrent token limits per user (e.g., max 5 devices).
 * Uses: Prevents token proliferation, improves revocation effectiveness.
 * 
 * Single Responsibility: Manages multi-device/concurrent token limits.
 * Dependency Inversion: Depends on RefreshTokenRepository abstraction.
 */
@Service
public class ConcurrentTokenLimitServiceImpl implements ConcurrentTokenLimitService {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentTokenLimitServiceImpl.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final int maxConcurrentTokens;

    public ConcurrentTokenLimitServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.max-concurrent-tokens:5}") int maxConcurrentTokens) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.maxConcurrentTokens = maxConcurrentTokens;
    }

    /**
     * Enforce concurrent token limit by revoking oldest token if limit exceeded.
     *
     * @param username User identifier
     * @return true if limit enforced (oldest token revoked), false if under limit
     */
    @Override
    @Transactional
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
}
