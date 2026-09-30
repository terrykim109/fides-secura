package com.bank.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "CAD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransferStatus status;

    @Column(name = "initiated_by", nullable = false)
    private Long initiatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Transfer() {
    }

    public Transfer(String idempotencyKey, Long fromAccountId, Long toAccountId,
                    BigDecimal amount, String currency, TransferStatus status,
                    Long initiatedBy) {
        this.idempotencyKey = idempotencyKey;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.initiatedBy = initiatedBy;
    }

    public void markPendingReview() {
        this.status = TransferStatus.PENDING_REVIEW;
    }

    public void markCompleted() {
        this.status = TransferStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = TransferStatus.FAILED;
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransferStatus getStatus() { return status; }
    public Long getInitiatedBy() { return initiatedBy; }
    public Instant getCreatedAt() { return createdAt; }
}