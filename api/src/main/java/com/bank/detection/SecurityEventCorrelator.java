package com.bank.detection;

import com.bank.domain.SecurityEvent;
import com.bank.security.SecurityEventTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Correlates auth telemetry with a transfer attempt.
 * Rule: several recent failures, then login success from a new IP, then a large transfer.
 */
public final class SecurityEventCorrelator {

    public static final String RULE_BRUTE_FORCE_THEN_LARGE_TRANSFER =
            "BRUTE_FORCE_THEN_LARGE_TRANSFER";

    private SecurityEventCorrelator() {
    }

    public record CorrelationHit(
            String ruleName,
            String title,
            String severity,
            String summary
    ) {
    }

    public record Evaluation(
            boolean hit,
            CorrelationHit details,
            List<Long> contributingEventIds
    ) {
        public static Evaluation miss() {
            return new Evaluation(false, null, List.of());
        }
    }

    public static CorrelationHit previewRule() {
        return new CorrelationHit(
                RULE_BRUTE_FORCE_THEN_LARGE_TRANSFER,
                "Possible account takeover",
                "HIGH",
                "Multiple failed logins, then success from a new IP, then a large transfer."
        );
    }

    /**
     * @param recentEvents         subject-user events in the lookback window (any order)
     * @param knownSuccessIps      LOGIN_SUCCESS IPs observed before the lookback window
     * @param transferAmount       amount being attempted
     * @param transferIp           IP of the transfer request
     * @param largeAmountThreshold amount at/above which transfers are "large"
     * @param minFailures          minimum LOGIN_FAILURE count before a successful login
     */
    public static Evaluation evaluate(
            List<SecurityEvent> recentEvents,
            Set<String> knownSuccessIps,
            BigDecimal transferAmount,
            String transferIp,
            BigDecimal largeAmountThreshold,
            int minFailures
    ) {
        if (transferAmount == null
                || largeAmountThreshold == null
                || transferAmount.compareTo(largeAmountThreshold) < 0) {
            return Evaluation.miss();
        }

        if (recentEvents == null || recentEvents.isEmpty() || minFailures <= 0) {
            return Evaluation.miss();
        }

        List<SecurityEvent> ordered = recentEvents.stream()
                .filter(event -> event != null && event.getCreatedAt() != null)
                .sorted(Comparator.comparing(SecurityEvent::getCreatedAt))
                .toList();

        List<SecurityEvent> failuresBeforeSuccess = new ArrayList<>();
        SecurityEvent successAfterFailures = null;

        for (SecurityEvent event : ordered) {
            if (SecurityEventTypes.LOGIN_FAILURE.equals(event.getEventType())) {
                failuresBeforeSuccess.add(event);
                continue;
            }

            if (SecurityEventTypes.LOGIN_SUCCESS.equals(event.getEventType())
                    && failuresBeforeSuccess.size() >= minFailures) {
                successAfterFailures = event;
                break;
            }
        }

        if (successAfterFailures == null) {
            return Evaluation.miss();
        }

        String successIp = successAfterFailures.getIpAddress();
        if (successIp == null || successIp.isBlank()) {
            return Evaluation.miss();
        }

        Set<String> known = knownSuccessIps == null ? Set.of() : knownSuccessIps;

        boolean newIp = true;
        for (String ip : known) {
            if (ip != null && ip.equalsIgnoreCase(successIp)) {
                newIp = false;
                break;
            }
        }

        if (!newIp) {
            return Evaluation.miss();
        }

        if (transferIp == null || !successIp.equalsIgnoreCase(transferIp)) {
            return Evaluation.miss();
        }

        List<Long> ids = new ArrayList<>();

        for (SecurityEvent failure : failuresBeforeSuccess) {
            if (failure.getId() != null) {
                ids.add(failure.getId());
            }
        }

        if (successAfterFailures.getId() != null) {
            ids.add(successAfterFailures.getId());
        }

        Instant successAt = successAfterFailures.getCreatedAt();

        CorrelationHit hit = new CorrelationHit(
                RULE_BRUTE_FORCE_THEN_LARGE_TRANSFER,
                "Possible account takeover",
                "HIGH",
                "Saw " + failuresBeforeSuccess.size()
                        + " failed logins before LOGIN_SUCCESS from new IP "
                        + successIp
                        + " at " + successAt
                        + ", followed by transfer of "
                        + transferAmount.toPlainString()
                        + " CAD."
        );

        return new Evaluation(true, hit, List.copyOf(ids));
    }

    public static Set<String> toIpSet(List<String> ips) {
        if (ips == null || ips.isEmpty()) {
            return Set.of();
        }

        Set<String> set = new HashSet<>();

        for (String ip : ips) {
            if (ip != null && !ip.isBlank()) {
                set.add(ip);
            }
        }

        return set;
    }
}