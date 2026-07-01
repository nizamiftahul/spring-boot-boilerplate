package com.example.spring_boot_boilerplate.security.policy;

/**
 * Strategy interface for token rotation policies.
 * Implements the Strategy pattern to support different rotation behaviors.
 * 
 * Uses: Determines when and how refresh tokens should be rotated
 * (e.g., always rotate, rotate on expiry, rotate after N uses).
 */
public interface TokenRotationPolicy {

    /**
     * Determine if a refresh token should be rotated.
     *
     * @param tokenAge Duration in milliseconds since token creation
     * @param tokenUsageCount Number of times token has been used (if tracked)
     * @return true if token should be rotated, false otherwise
     */
    boolean shouldRotate(long tokenAge, int tokenUsageCount);

    /**
     * Get policy name for logging/auditing.
     *
     * @return Policy identifier
     */
    String getPolicyName();
}
