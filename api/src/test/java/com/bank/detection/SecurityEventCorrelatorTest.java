package com.bank.detection;

import com.bank.domain.SecurityEvent;
import com.bank.domain.SecuritySeverity;
import com.bank.security.SecurityEventTypes;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityEventCorrelatorTest {

    private static final BigDecimal THRESHOLD = new BigDecimal("5000.00");

    @Test
    void hitsWhenFailuresThenNewIpSuccessThenLargeTransfer() {
        Instant t0 = Instant.parse("2026-09-29T04:00:00Z");
        List<SecurityEvent> events = List.of(
                event(1L, SecurityEventTypes.LOGIN_FAILURE, "10.0.0.1", t0),
                event(2L, SecurityEventTypes.LOGIN_FAILURE, "10.0.0.1", t0.plusSeconds(30)),
                event(3L, SecurityEventTypes.LOGIN_FAILURE, "10.0.0.1", t0.plusSeconds(60)),
                event(4L, SecurityEventTypes.LOGIN_SUCCESS, "203.0.113.50", t0.plusSeconds(120))
        );

        var result = SecurityEventCorrelator.evaluate(
                events,
                Set.of("192.168.1.10"),
                new BigDecimal("5000.00"),
                "203.0.113.50",
                THRESHOLD,
                3
        );

        assertTrue(result.hit());
        assertTrue(result.contributingEventIds().contains(4L));
    }

    @Test
    void missesWhenAmountBelowThreshold() {
        Instant t0 = Instant.parse("2026-09-29T04:00:00Z");
        List<SecurityEvent> events = List.of(
                event(1L, SecurityEventTypes.LOGIN_FAILURE, "10.0.0.1", t0),
                event(2L, SecurityEventTypes.LOGIN_FAILURE, "10.0.0.1", t0.plusSeconds(30)),
                event(3L, SecurityEventTypes.LOGIN_FAILURE, "10.0.0.1", t0.plusSeconds(60)),
                event(4L, SecurityEventTypes.LOGIN_SUCCESS, "203.0.113.50", t0.plusSeconds(120))
        );

        var result = SecurityEventCorrelator.evaluate(
                events,
                Set.of(),
                new BigDecimal("4999.99"),
                "203.0.113.50",
                THRESHOLD,
                3
        );

        assertFalse(result.hit());
    }

    @Test
    void missesWhenSuccessIpIsKnown() {
        Instant t0 = Instant.parse("2026-09-29T04:00:00Z");
        List<SecurityEvent> events = List.of(
                event(1L, SecurityEventTypes.LOGIN_FAILURE, "10.0.0.1", t0),
                event(2L, SecurityEventTypes.LOGIN_FAILURE, "10.0.0.1", t0.plusSeconds(30)),
                event(3L, SecurityEventTypes.LOGIN_FAILURE, "10.0.0.1", t0.plusSeconds(60)),
                event(4L, SecurityEventTypes.LOGIN_SUCCESS, "192.168.1.10", t0.plusSeconds(120))
        );

        var result = SecurityEventCorrelator.evaluate(
                events,
                Set.of("192.168.1.10"),
                new BigDecimal("8000.00"),
                "192.168.1.10",
                THRESHOLD,
                3
        );

        assertFalse(result.hit());
    }

    private static SecurityEvent event(Long id, String type, String ip, Instant at) {
        SecurityEvent e = new SecurityEvent(
                type,
                SecuritySeverity.INFO,
                1L,
                1L,
                ip,
                "test",
                null,
                null
        );
        try {
            var field = SecurityEvent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(e, id);
            var created = SecurityEvent.class.getDeclaredField("createdAt");
            created.setAccessible(true);
            created.set(e, at);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return e;
    }
}
