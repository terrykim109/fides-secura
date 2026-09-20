package com.bank.service;

import com.bank.config.AppProperties;
import com.bank.security.SecurityEventTypes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory per-IP login throttle for the demo.
 * Restart clears counters — fine for local portfolio use, not a distributed limiter.
 */
@Component
public class LoginRateLimiter {

    private final AppProperties appProperties;
    private final Map<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();

    public LoginRateLimiter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void checkOrThrow(String ipAddress) {
        int limit = appProperties.auth().loginRateLimitPerIp();
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(60);
        Deque<Instant> q = attemptsByIp.computeIfAbsent(ipAddress, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst().isBefore(windowStart)) {
                q.removeFirst();
            }
            if (q.size() >= limit) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, SecurityEventTypes.RATE_LIMITED);
            }
            q.addLast(now);
        }
    }
}
