package com.bank.detection;

/**
 * Placeholder for ACCOUNT_TAKEOVER_V1. Real correlation belongs with security_events queries + tests.
 */
public final class SecurityEventCorrelator {

    public static final String RULE_BRUTE_FORCE_THEN_TRANSFER = "BRUTE_FORCE_THEN_LARGE_TRANSFER";

    private SecurityEventCorrelator() {
    }

    public record CorrelationHit(
            String ruleName,
            String title,
            String severity,
            String summary
    ) {
    }

    /** Demo preview only — not backed by persisted events. */
    public static CorrelationHit previewRule() {
        return new CorrelationHit(
                RULE_BRUTE_FORCE_THEN_TRANSFER,
                "Possible account takeover",
                "HIGH",
                "Multiple failed logins, then success from a new IP, then a large transfer."
        );
    }
}
