package com.bank.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Value("${spring.application.name:fides-secura}")
    private String serviceName;

    @GetMapping
    public Map<String, String> health() {
        Map<String, String> body = new HashMap<>();
        body.put("status", "UP");
        body.put("service", serviceName);
        body.put("focus", "secure-transfers-ato");
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}