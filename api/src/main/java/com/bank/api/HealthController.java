// package com.bank.api;

// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import java.time.Instant;
// import java.util.Map;

// @RestController
// @RequestMapping("/api/v1/health")
// public class HealthController {

//     @GetMapping
//     public ResponseEntity<Map<String, Object>> health() {
//         return ResponseEntity.ok(Map.of(
//                 "status", "UP",
//                 "service", "fides-secura",
//                 "focus", "cybersecurity",
//                 "timestamp", Instant.now().toString()
//         ));
//     }
// }

package com.bank.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Value("${spring.application.name:fides-secura}")
    private String serviceName;

    // lightweight liveness only — real dependency checks live in /actuator/health
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("status", "UP", "service", serviceName));
    }
}