package com.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        Auth auth
) {
    public record Jwt(
            String secret,
            long accessTokenMinutes,
            long refreshTokenDays
    ) {
    }

    public record Cors(
            String allowedOrigins
    ) {
    }

    public record Auth(
            int maxFailedLogins,
            long lockoutMinutes,
            int loginRateLimitPerIp
    ) {
    }
}
