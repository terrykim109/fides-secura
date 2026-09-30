package com.bank.service;

import com.bank.config.AppProperties;
import com.bank.detection.SecurityEventCorrelator;
import com.bank.domain.Incident;
import com.bank.domain.IncidentEvent;
import com.bank.domain.SecurityEvent;
import com.bank.domain.SecuritySeverity;
import com.bank.repository.IncidentEventRepository;
import com.bank.repository.IncidentRepository;
import com.bank.repository.SecurityEventRepository;
import com.bank.security.SecurityEventTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class AtoDetectionService {

    private final SecurityEventRepository securityEventRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final SecurityEventRecorder securityEventRecorder;
    private final AppProperties appProperties;

    public AtoDetectionService(
            SecurityEventRepository securityEventRepository,
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            SecurityEventRecorder securityEventRecorder,
            AppProperties appProperties
    ) {
        this.securityEventRepository = securityEventRepository;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.securityEventRecorder = securityEventRecorder;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public SecurityEventCorrelator.Evaluation evaluate(Long userId, BigDecimal amount, String transferIp) {
        Instant since = Instant.now().minus(appProperties.detection().lookbackMinutes(), ChronoUnit.MINUTES);
        List<SecurityEvent> recent = securityEventRepository.findBySubjectSince(userId, since);
        Set<String> knownIps = SecurityEventCorrelator.toIpSet(
                securityEventRepository.findDistinctIpsBefore(userId, SecurityEventTypes.LOGIN_SUCCESS, since)
        );
        return SecurityEventCorrelator.evaluate(
                recent,
                knownIps,
                amount,
                transferIp,
                appProperties.detection().largeTransferThreshold(),
                appProperties.detection().minFailedLogins()
        );
    }

    @Transactional
    public Incident openIncident(
            Long userId,
            Long transferId,
            SecurityEventCorrelator.Evaluation evaluation,
            String ipAddress,
            String userAgent
    ) {
        var details = evaluation.details();
        Incident incident = incidentRepository.save(new Incident(
                details.title(),
                SecuritySeverity.HIGH,
                details.ruleName(),
                details.summary(),
                userId,
                transferId
        ));

        for (Long eventId : evaluation.contributingEventIds()) {
            incidentEventRepository.save(new IncidentEvent(incident.getId(), eventId));
        }

        securityEventRecorder.record(
                SecurityEventTypes.TRANSFER_FLAGGED,
                SecuritySeverity.HIGH,
                userId,
                userId,
                ipAddress,
                userAgent,
                "incident-" + incident.getId(),
                "{\"transferId\":" + transferId
                        + ",\"incidentId\":" + incident.getId()
                        + ",\"rule\":\"" + details.ruleName() + "\"}"
        );

        return incident;
    }
}
