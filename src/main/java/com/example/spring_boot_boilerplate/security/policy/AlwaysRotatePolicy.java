package com.example.spring_boot_boilerplate.security.policy;

/**
 * Always rotate refresh tokens on every use.
 * Highest security (token reuse impossible), but impacts performance/UX.
 */
public class AlwaysRotatePolicy implements TokenRotationPolicy {

    @Override
    public boolean shouldRotate(long tokenAge, int tokenUsageCount) {
        return true; // Always rotate
    }

    @Override
    public String getPolicyName() {
        return "ALWAYS_ROTATE";
    }
}
