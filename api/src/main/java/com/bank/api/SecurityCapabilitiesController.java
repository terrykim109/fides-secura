package com.bank.api;

import com.bank.detection.SecurityEventCorrelator;
import com.bank.fraud.FraudRuleEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityCapabilitiesController {

    @GetMapping("/capabilities")
    public Map<String, Object> capabilities() {
        var fraud = FraudRuleEngine.assess(new BigDecimal("7500.00"), 1, true);
        var correlation = SecurityEventCorrelator.previewRule();

        Map<String, Object> fraudSample = new HashMap<>();
        fraudSample.put("riskScore", fraud.riskScore());
        fraudSample.put("reasons", fraud.reasons());
        fraudSample.put("flagged", fraud.flagged());

        Map<String, Object> correlationSample = new HashMap<>();
        correlationSample.put("ruleName", correlation.ruleName());
        correlationSample.put("title", correlation.title());
        correlationSample.put("severity", correlation.severity());
        correlationSample.put("summary", correlation.summary());

        Map<String, Object> body = new HashMap<>();
        body.put("product", "Fides Secura");
        body.put("status", "ato-correlation-live");
        body.put("note", "Live ATO rule holds large transfers after failed-login -> new-IP success.");
        body.put("fraudSample", fraudSample);
        body.put("correlationSample", correlationSample);
        body.put("roles", List.of("CUSTOMER", "TELLER", "ANALYST", "ADMIN"));
        return body;
    }
}