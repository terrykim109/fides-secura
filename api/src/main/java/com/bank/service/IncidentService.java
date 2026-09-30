package com.bank.service;

import com.bank.api.dto.IncidentResponse;
import com.bank.api.dto.TransferResponse;
import com.bank.domain.Account;
import com.bank.domain.Incident;
import com.bank.domain.IncidentStatus;
import com.bank.domain.SecuritySeverity;
import com.bank.domain.Transfer;
import com.bank.domain.TransferStatus;
import com.bank.repository.AccountRepository;
import com.bank.repository.IncidentRepository;
import com.bank.repository.TransferRepository;
import com.bank.security.SecurityEventTypes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final SecurityEventRecorder securityEventRecorder;

    public IncidentService(
            IncidentRepository incidentRepository,
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            SecurityEventRecorder securityEventRecorder
    ) {
        this.incidentRepository = incidentRepository;
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.securityEventRecorder = securityEventRecorder;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listOpen() {
        return incidentRepository.findByStatusOrderByCreatedAtDesc(IncidentStatus.OPEN).stream()
                .map(IncidentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listAll() {
        return incidentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(IncidentResponse::from)
                .toList();
    }

    @Transactional
    public TransferResponse approve(Long incidentId, Long analystUserId) {
        Incident incident = requireOpen(incidentId);
        Transfer transfer = requireHeldTransfer(incident);

        Long firstId = Math.min(transfer.getFromAccountId(), transfer.getToAccountId());
        Long secondId = Math.max(transfer.getFromAccountId(), transfer.getToAccountId());
        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        Account from = first.getId().equals(transfer.getFromAccountId()) ? first : second;
        Account to = first.getId().equals(transfer.getToAccountId()) ? first : second;

        from.debit(transfer.getAmount());
        to.credit(transfer.getAmount());
        accountRepository.save(from);
        accountRepository.save(to);

        transfer.markCompleted();
        transferRepository.save(transfer);
        incident.close("Approved by analyst " + analystUserId + "; transfer completed");
        incidentRepository.save(incident);

        securityEventRecorder.record(
                SecurityEventTypes.FRAUD_REVIEW_DECISION,
                SecuritySeverity.INFO,
                analystUserId,
                incident.getSubjectUserId(),
                null,
                null,
                "incident-" + incident.getId(),
                "{\"decision\":\"APPROVED\",\"transferId\":" + transfer.getId() + "}"
        );

        return TransferResponse.from(transfer);
    }

    @Transactional
    public TransferResponse reject(Long incidentId, Long analystUserId) {
        Incident incident = requireOpen(incidentId);
        Transfer transfer = requireHeldTransfer(incident);
        transfer.markFailed();
        transferRepository.save(transfer);
        incident.contain("Rejected by analyst " + analystUserId + "; balances unchanged");
        incidentRepository.save(incident);

        securityEventRecorder.record(
                SecurityEventTypes.FRAUD_REVIEW_DECISION,
                SecuritySeverity.MEDIUM,
                analystUserId,
                incident.getSubjectUserId(),
                null,
                null,
                "incident-" + incident.getId(),
                "{\"decision\":\"REJECTED\",\"transferId\":" + transfer.getId() + "}"
        );

        return TransferResponse.from(transfer);
    }

    private Incident requireOpen(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
        if (incident.getStatus() != IncidentStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Incident is not open");
        }
        return incident;
    }

    private Transfer requireHeldTransfer(Incident incident) {
        if (incident.getTransferId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Incident has no linked transfer");
        }
        Transfer transfer = transferRepository.findById(incident.getTransferId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found"));
        if (transfer.getStatus() != TransferStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transfer is not pending review");
        }
        return transfer;
    }
}
