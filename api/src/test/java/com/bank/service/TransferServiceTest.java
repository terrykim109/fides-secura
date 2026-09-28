package com.bank.service;

import com.bank.api.dto.CreateTransferRequest;
import com.bank.api.dto.TransferResponse;
import com.bank.domain.Account;
import com.bank.domain.AccountType;
import com.bank.domain.Transfer;
import com.bank.domain.TransferStatus;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransferRepository;
import com.bank.security.SecurityEventTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    TransferRepository transferRepository;
    @Mock
    AccountRepository accountRepository;
    @Mock
    SecurityEventRecorder securityEventRecorder;
    @Mock
    TransactionTemplate transactionTemplate;

    TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(transferRepository, accountRepository,
                securityEventRecorder, transactionTemplate);
        when(transactionTemplate.execute(any())).thenAnswer((Answer<Object>) invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            return callback.doInTransaction(status);
        });
    }

    @Test
    void replaySameIdempotencyKeyDoesNotMoveMoneyAgain() {
        Transfer existing = new Transfer("key-1", 1L, 2L, new BigDecimal("25.00"),
                "CAD", TransferStatus.COMPLETED, 10L);
        when(transferRepository.findByIdempotencyKeyAndInitiatedBy("key-1", 10L))
                .thenReturn(Optional.of(existing));

        TransferResponse response = transferService.transfer(10L, "key-1",
                new CreateTransferRequest(1L, 2L, new BigDecimal("25.00")), "127.0.0.1", "test");

        assertEquals("key-1", response.idempotencyKey());
        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(securityEventRecorder, never()).record(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void successfulTransferDebitsAndCreditsInLockOrder() {
        Account from = new Account(10L, "FSFROM", AccountType.CHECKING, new BigDecimal("100.00"));
        Account to = new Account(20L, "FSTO", AccountType.CHECKING, new BigDecimal("5.00"));
        setId(from, 1L);
        setId(to, 2L);

        when(transferRepository.findByIdempotencyKeyAndInitiatedBy("k2", 10L)).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(to));
        when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferResponse response = transferService.transfer(10L, "k2",
                new CreateTransferRequest(1L, 2L, new BigDecimal("30.00")), "127.0.0.1", "test");

        assertEquals(TransferStatus.COMPLETED, response.status());
        assertEquals(0, from.getBalance().compareTo(new BigDecimal("70.00")));
        assertEquals(0, to.getBalance().compareTo(new BigDecimal("35.00")));
        verify(securityEventRecorder).record(eq(SecurityEventTypes.TRANSFER_COMPLETED), any(),
                eq(10L), eq(10L), eq("127.0.0.1"), eq("test"), any(), any());
    }

    private static void setId(Account account, Long id) {
        try {
            var field = Account.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}