package com.example.spring_boot_boilerplate.security.policy;

/**
 * No rotation: reuse tokens indefinitely until expiry.
 * Lowest security but best performance/UX.
 */
public class NoRotatePolicy implements TokenRotationPolicy {

    @Override
    public boolean shouldRotate(long tokenAge, int tokenUsageCount) {
        return false; // Never rotate
    }

    @Override
    public String getPolicyName() {
        return "NO_ROTATE";
    }
}
