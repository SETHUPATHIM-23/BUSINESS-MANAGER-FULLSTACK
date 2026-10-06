package com.businessmanager.backend.fund.dto;

import com.businessmanager.backend.fund.entity.FundTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundTransactionResponseDto {
    private Long id;
    private Long fundAccountId;
    private String fundAccountName;
    private String fundAccountNumber;
    private FundTransactionType type;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String referenceDocumentType;
    private Long referenceDocumentId;
    private Long targetFundAccountId;
    private String targetFundAccountName;
    private String description;
    private LocalDateTime createdAt;
}
