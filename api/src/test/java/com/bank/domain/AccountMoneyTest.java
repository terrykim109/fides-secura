package com.bank.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountMoneyTest {

    @Test
    void debitAndCreditUpdateBalance() {
        Account account = new Account(1L, "FS1", AccountType.CHECKING, new BigDecimal("100.00"));
        account.debit(new BigDecimal("40.00"));
        assertEquals(0, account.getBalance().compareTo(new BigDecimal("60.00")));
        account.credit(new BigDecimal("15.50"));
        assertEquals(0, account.getBalance().compareTo(new BigDecimal("75.50")));
    }

    @Test
    void debitRejectsInsufficientFunds() {
        Account account = new Account(1L, "FS1", AccountType.CHECKING, new BigDecimal("10.00"));
        var ex = assertThrows(IllegalStateException.class,
                () -> account.debit(new BigDecimal("10.01")));
        assertEquals("Insufficient funds", ex.getMessage());
        assertEquals(0, account.getBalance().compareTo(new BigDecimal("10.00")));
    }
}