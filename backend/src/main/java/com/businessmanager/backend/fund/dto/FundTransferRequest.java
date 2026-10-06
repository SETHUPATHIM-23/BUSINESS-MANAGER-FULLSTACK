package com.businessmanager.backend.fund.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundTransferRequest {

    @NotNull(message = "Source fund account ID is required")
    private Long sourceAccountId;

    @NotNull(message = "Target fund account ID is required")
    private Long targetAccountId;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
    private BigDecimal amount;

    private LocalDate transactionDate;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
