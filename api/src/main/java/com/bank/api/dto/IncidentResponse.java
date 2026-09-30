package com.bank.api.dto;

import com.bank.domain.Incident;
import com.bank.domain.IncidentStatus;
import com.bank.domain.SecuritySeverity;

import java.time.Instant;

public record IncidentResponse(
        Long id,
        String title,
        SecuritySeverity severity,
        IncidentStatus status,
        String ruleName,
        String summary,
        Long subjectUserId,
        Long transferId,
        Instant createdAt
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getRuleName(),
                incident.getSummary(),
                incident.getSubjectUserId(),
                incident.getTransferId(),
                incident.getCreatedAt()
        );
    }
}
