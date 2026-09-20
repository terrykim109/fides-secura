package com.bank.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserLockoutTest {

    @Test
    void fifthFailedLoginLocksAccount() {
        User user = new User("a@b.com", "hash", "A", Role.CUSTOMER);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        user.registerFailedLogin(5, 15, now);
        user.registerFailedLogin(5, 15, now);
        user.registerFailedLogin(5, 15, now);
        user.registerFailedLogin(5, 15, now);
        assertFalse(user.isLocked(now));

        user.registerFailedLogin(5, 15, now);
        assertTrue(user.isLocked(now));
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(now));
    }
}
