package com.bank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SecuritySeverity severity = SecuritySeverity.INFO;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "subject_user_id")
    private Long subjectUserId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected SecurityEvent() {
    }

    public SecurityEvent(
            String eventType,
            SecuritySeverity severity,
            Long actorUserId,
            Long subjectUserId,
            String ipAddress,
            String userAgent,
            String correlationId,
            String payload
    ) {
        this.eventType = eventType;
        this.severity = severity;
        this.actorUserId = actorUserId;
        this.subjectUserId = subjectUserId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.correlationId = correlationId;
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public SecuritySeverity getSeverity() {
        return severity;
    }

    public Long getSubjectUserId() {
        return subjectUserId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
