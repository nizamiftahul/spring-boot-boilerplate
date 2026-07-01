package com.example.spring_boot_boilerplate.auth;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service to clean up expired refresh tokens.
 * Uses: Prevents token table bloat, reduces database size over time.
 * 
 * @ConditionalOnProperty: Enables scheduled cleanup only if
 *                         jwt.cleanup-enabled=true.
 *                         @Scheduled(cron): Runs daily at 2 AM UTC by default
 *                         (configurable).
 *                         Single Responsibility: Manages token expiration
 *                         cleanup.
 */
@Service
@ConditionalOnProperty(name = "jwt.cleanup-enabled", havingValue = "true", matchIfMissing = false)
public class TokenCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public TokenCleanupService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        logger.info("TokenCleanupService initialized and scheduled");
    }

    /**
     * Scheduled cleanup job to delete expired refresh tokens.
     * Runs daily at 2 AM UTC (hardcoded; override via properties if needed).
     */
    @Scheduled(cron = "0 2 * * * *", zone = "UTC")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();

        // Get all tokens (inefficient for large tables; in production, add DB-level
        // cleanup or pagination)
        Iterable<RefreshToken> allTokens = refreshTokenRepository.findAll();
        int deletedCount = 0;

        for (RefreshToken token : allTokens) {
            if (token.getExpiryDate().isBefore(now)) {
                refreshTokenRepository.delete(token);
                deletedCount++;
            }
        }

        if (deletedCount > 0) {
            logger.info("Cleanup job: Deleted {} expired refresh tokens", deletedCount);
        } else {
            logger.debug("Cleanup job: No expired tokens found");
        }
    }
}
