package com.bank.api;

import com.bank.detection.SecurityEventCorrelator;
import com.bank.fraud.FraudRuleEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityCapabilitiesController {

    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> capabilities() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("product", "Fides Secura");
        body.put("status", "auth-slice");
        body.put("note", "Auth/lockout live; transfers and ATO correlator not wired yet");
        body.put("fraudSample", FraudRuleEngine.assess(new BigDecimal("7500.00"), 4, true));
        body.put("correlationSample", SecurityEventCorrelator.previewRule());
        body.put("rolesPlanned", new String[]{"CUSTOMER", "TELLER", "ANALYST", "ADMIN"});
        return ResponseEntity.ok(body);
    }
}
