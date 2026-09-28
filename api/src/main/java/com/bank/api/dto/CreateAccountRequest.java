package com.bank.api.dto;

import com.bank.domain.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotNull AccountType accountType,
        @DecimalMin(value = "0.00", inclusive = true) BigDecimal initialDeposit
) {
}
