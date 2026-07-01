package com.example.spring_boot_boilerplate.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_boilerplate.auth.entity.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token Repository.
 * 
 * Data access interface for RefreshToken persistence.
 * Abstracts database operations (Dependency Inversion principle).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a refresh token by its token string.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all refresh tokens for a specific user.
     */
    Iterable<RefreshToken> findByUsername(String username);

    /**
     * Delete all refresh tokens for a specific user (logout all devices).
     */
    void deleteByUsername(String username);

    /**
     * Delete all refresh tokens that have expired before the given time.
     */
    long deleteByExpiresAtBefore(Instant now);
}
