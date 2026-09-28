package com.bank.api.dto;

import com.bank.domain.Transfer;
import com.bank.domain.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long id,
        String idempotencyKey,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        Instant createdAt
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getIdempotencyKey(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getCreatedAt()
        );
    }
}
