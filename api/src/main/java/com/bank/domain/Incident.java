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
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SecuritySeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "subject_user_id")
    private Long subjectUserId;

    @Column(name = "transfer_id")
    private Long transferId;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Incident() {
    }

    public Incident(
            String title,
            SecuritySeverity severity,
            String ruleName,
            String summary,
            Long subjectUserId,
            Long transferId
    ) {
        this.title = title;
        this.severity = severity;
        this.ruleName = ruleName;
        this.summary = summary;
        this.subjectUserId = subjectUserId;
        this.transferId = transferId;
    }

    public void contain(String note) {
        this.status = IncidentStatus.CONTAINED;
        this.summary = this.summary + " | " + note;
        this.updatedAt = Instant.now();
    }

    public void close(String note) {
        this.status = IncidentStatus.CLOSED;
        this.summary = this.summary + " | " + note;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public SecuritySeverity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getSummary() {
        return summary;
    }

    public Long getSubjectUserId() {
        return subjectUserId;
    }

    public Long getTransferId() {
        return transferId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
