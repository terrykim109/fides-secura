package com.bank.fraud;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure scoring helper. Callers must persist signal breakdowns; this class does not touch the DB.
 */
public final class FraudRuleEngine {

    public static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("5000.00");
    public static final int VELOCITY_WINDOW_MINUTES = 10;
    public static final int VELOCITY_MAX_TRANSFERS = 3;

    private FraudRuleEngine() {
    }

    public record FraudAssessment(int riskScore, List<String> reasons, boolean flagged) {
    }

    public static FraudAssessment assess(BigDecimal amount, int transfersInWindow, boolean newIp) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            score += 40;
            reasons.add("Amount >= " + HIGH_AMOUNT_THRESHOLD + " CAD");
        }
        if (transfersInWindow >= VELOCITY_MAX_TRANSFERS) {
            score += 35;
            reasons.add("Velocity: " + transfersInWindow + " transfers in "
                    + VELOCITY_WINDOW_MINUTES + " minutes");
        }
        if (newIp) {
            score += 25;
            reasons.add("Transfer from new or uncommon IP for this customer");
        }

        int capped = Math.min(score, 100);
        return new FraudAssessment(capped, List.copyOf(reasons), capped >= 60);
    }
}
