package com.businessmanager.backend.accounting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class JournalLineRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Debit amount is required")
    @DecimalMin(value = "0.00", message = "Debit amount must be non-negative")
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "0.00", message = "Credit amount must be non-negative")
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Size(max = 255, message = "Line description cannot exceed 255 characters")
    private String description;
}
