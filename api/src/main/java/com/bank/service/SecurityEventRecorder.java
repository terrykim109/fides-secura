package com.bank.service;

import com.bank.domain.SecurityEvent;
import com.bank.domain.SecuritySeverity;
import com.bank.repository.SecurityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityEventRecorder {

    private final SecurityEventRepository securityEventRepository;

    public SecurityEventRecorder(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    @Transactional
    public SecurityEvent record(
            String eventType,
            SecuritySeverity severity,
            Long actorUserId,
            Long subjectUserId,
            String ipAddress,
            String userAgent,
            String correlationId,
            String payload
    ) {
        return securityEventRepository.save(new SecurityEvent(
                eventType,
                severity,
                actorUserId,
                subjectUserId,
                ipAddress,
                userAgent,
                correlationId,
                payload
        ));
    }
}
