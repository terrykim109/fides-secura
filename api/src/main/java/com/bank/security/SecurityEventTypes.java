package com.bank.security;

/** Stable event type strings for security_events. Correlator rules depend on these exact values. */
public final class SecurityEventTypes {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String TOKEN_REFRESH = "TOKEN_REFRESH";
    public static final String TRANSFER_COMPLETED = "TRANSFER_COMPLETED";
    public static final String TRANSFER_FLAGGED = "TRANSFER_FLAGGED";
    public static final String FRAUD_REVIEW_DECISION = "FRAUD_REVIEW_DECISION";

    private SecurityEventTypes() {
    }
}
