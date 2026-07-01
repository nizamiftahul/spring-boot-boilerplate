package com.example.spring_boot_boilerplate.auth.service;

import org.springframework.transaction.annotation.Transactional;

/**
 * Enforces concurrent token limits per user (e.g., max 5 devices).
 * Uses: Prevents token proliferation, improves revocation effectiveness.
 * 
 * Single Responsibility: Manages multi-device/concurrent token limits.
 * Dependency Inversion: Depends on RefreshTokenRepository abstraction.
 */
public interface ConcurrentTokenLimitService {
    /**
     * Enforce concurrent token limit by revoking oldest token if limit exceeded.
     *
     * @param username User identifier
     * @return true if limit enforced (oldest token revoked), false if under limit
     */
    @Transactional
    public boolean enforceConcurrentLimit(String username);

    public int getMaxConcurrentTokens();
}
