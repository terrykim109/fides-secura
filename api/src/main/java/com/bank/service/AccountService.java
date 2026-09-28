package com.bank.service;

import com.bank.api.dto.AccountResponse;
import com.bank.api.dto.CreateAccountRequest;
import com.bank.api.dto.DepositRequest;
import com.bank.domain.Account;
import com.bank.domain.AccountType;
import com.bank.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse open(Long customerId, CreateAccountRequest request) {
        AccountType type = request.accountType();
        BigDecimal opening = request.initialDeposit() == null ? BigDecimal.ZERO : request.initialDeposit();
        if (opening.compareTo(new BigDecimal("1000000.00")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Initial deposit too large for demo");
        }
        Account account = new Account(customerId, nextAccountNumber(), type, opening);
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listMine(Long customerId) {
        return accountRepository.findByCustomerIdOrderByIdAsc(customerId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getMine(Long customerId, Long accountId) {
        Account account = accountRepository.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse deposit(Long customerId, Long accountId, DepositRequest request) {
        Account account = accountRepository.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        account.credit(request.amount());
        return AccountResponse.from(accountRepository.save(account));
    }

    private static String nextAccountNumber() {
        return "FS" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}