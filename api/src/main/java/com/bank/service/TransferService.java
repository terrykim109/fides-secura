package com.bank.service;

import com.bank.api.dto.CreateTransferRequest;
import com.bank.api.dto.TransferResponse;
import com.bank.detection.SecurityEventCorrelator;
import com.bank.domain.Account;
import com.bank.domain.SecuritySeverity;
import com.bank.domain.Transfer;
import com.bank.domain.TransferStatus;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransferRepository;
import com.bank.security.SecurityEventTypes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final SecurityEventRecorder securityEventRecorder;
    private final AtoDetectionService atoDetectionService;
    private final TransactionTemplate transactionTemplate;

    public TransferService(
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            SecurityEventRecorder securityEventRecorder,
            AtoDetectionService atoDetectionService,
            TransactionTemplate transactionTemplate
    ) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.securityEventRecorder = securityEventRecorder;
        this.atoDetectionService = atoDetectionService;
        this.transactionTemplate = transactionTemplate;
    }

    public TransferResponse transfer(
            Long userId,
            String idempotencyKey,
            CreateTransferRequest request,
            String ipAddress,
            String userAgent
    ) {
        String key = normalizeKey(idempotencyKey);
        try {
            return transactionTemplate.execute(status ->
                    executeTransfer(userId, key, request, ipAddress, userAgent, status)
            );
        } catch (IdempotencyRaceException ex) {
            return TransferResponse.from(
                    transferRepository.findByIdempotencyKeyAndInitiatedBy(key, userId)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "Transfer conflict; retry with the same Idempotency-Key"
                            ))
            );
        }
    }

    private TransferResponse executeTransfer(
            Long userId,
            String key,
            CreateTransferRequest request,
            String ipAddress,
            String userAgent,
            TransactionStatus status
    ) {
        var existing = transferRepository.findByIdempotencyKeyAndInitiatedBy(key, userId);
        if (existing.isPresent()) {
            return TransferResponse.from(existing.get());
        }

        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer to the same account");
        }

        BigDecimal amount = request.amount();
        Long firstId = Math.min(request.fromAccountId(), request.toAccountId());
        Long secondId = Math.max(request.fromAccountId(), request.toAccountId());

        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Account from = first.getId().equals(request.fromAccountId()) ? first : second;
        Account to = first.getId().equals(request.toAccountId()) ? first : second;

        if (!from.getCustomerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of the source account");
        }
        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency mismatch");
        }

        SecurityEventCorrelator.Evaluation evaluation =
                atoDetectionService.evaluate(userId, amount, ipAddress);

        if (evaluation.hit()) {
            // Hold: record transfer without moving balances until analyst decision.
            Transfer held = new Transfer(
                    key,
                    from.getId(),
                    to.getId(),
                    amount,
                    from.getCurrency(),
                    TransferStatus.PENDING_REVIEW,
                    userId
            );
            try {
                transferRepository.saveAndFlush(held);
            } catch (DataIntegrityViolationException ex) {
                status.setRollbackOnly();
                throw new IdempotencyRaceException(ex);
            }
            atoDetectionService.openIncident(userId, held.getId(), evaluation, ipAddress, userAgent);
            return TransferResponse.from(held);
        }

        from.debit(amount);
        to.credit(amount);
        accountRepository.save(from);
        accountRepository.save(to);

        Transfer transfer = new Transfer(
                key,
                from.getId(),
                to.getId(),
                amount,
                from.getCurrency(),
                TransferStatus.COMPLETED,
                userId
        );

        try {
            transferRepository.saveAndFlush(transfer);
        } catch (DataIntegrityViolationException ex) {
            status.setRollbackOnly();
            throw new IdempotencyRaceException(ex);
        }

        securityEventRecorder.record(
                SecurityEventTypes.TRANSFER_COMPLETED,
                SecuritySeverity.INFO,
                userId,
                from.getCustomerId(),
                ipAddress,
                userAgent,
                null,
                "{\"transferId\":" + transfer.getId()
                        + ",\"fromAccountId\":" + from.getId()
                        + ",\"toAccountId\":" + to.getId()
                        + ",\"amount\":\"" + amount.toPlainString() + "\"}"
        );

        return TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> listMine(Long userId) {
        List<Long> accountIds = accountRepository.findByCustomerIdOrderByIdAsc(userId).stream()
                .map(Account::getId)
                .toList();
        List<Long> ids = accountIds.isEmpty() ? List.of(-1L) : accountIds;
        return transferRepository.findVisibleToUser(userId, ids).stream()
                .map(TransferResponse::from)
                .toList();
    }

    private static String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }
        String key = idempotencyKey.trim();
        if (key.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key too long");
        }
        return key;
    }

    private static final class IdempotencyRaceException extends RuntimeException {
        private IdempotencyRaceException(Throwable cause) {
            super(cause);
        }
    }
}
