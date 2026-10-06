package com.businessmanager.backend.fund.dto;

import com.businessmanager.backend.fund.entity.FundTransactionType;
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
public class FundTransactionRequest {

    @NotNull(message = "Fund account ID is required")
    private Long fundAccountId;

    @NotNull(message = "Transaction type is required (RECEIPT or PAYMENT)")
    private FundTransactionType type;

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Transaction amount must be greater than zero")
    private BigDecimal amount;

    private LocalDate transactionDate;

    @Size(max = 50, message = "Reference document type must not exceed 50 characters")
    private String referenceDocumentType;

    private Long referenceDocumentId;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
