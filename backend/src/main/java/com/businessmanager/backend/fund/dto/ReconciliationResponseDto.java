package com.businessmanager.backend.fund.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResponseDto {
    private Long id;
    private Long fundTransactionId;
    private String bankStatementLineRef;
    private Boolean matched;
    private LocalDateTime reconciledAt;
    private String reconciledBy;
    private LocalDateTime createdAt;
}
