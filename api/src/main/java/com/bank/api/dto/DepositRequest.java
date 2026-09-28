package com.bank.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount
) {
}
