package com.businessmanager.backend.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TrialBalanceDto {
    private LocalDate asOfDate;
    
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private boolean isBalanced;
    
    private List<TrialBalanceLine> lines;

    @Data
    @Builder
    public static class TrialBalanceLine {
        private String accountCode;
        private String accountName;
        private String accountType;
        private BigDecimal debitBalance;
        private BigDecimal creditBalance;
    }
}
