package com.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Auth auth, Detection detection) {

    public record Jwt(String secret, long accessTokenMinutes, long refreshTokenDays) {
    }

    public record Cors(String allowedOrigins) {
    }

    public record Auth(int maxFailedLogins, long lockoutMinutes, int loginRateLimitPerIp) {
    }

    public record Detection(
            int lookbackMinutes,
            int minFailedLogins,
            java.math.BigDecimal largeTransferThreshold
    ) {
    }
}
