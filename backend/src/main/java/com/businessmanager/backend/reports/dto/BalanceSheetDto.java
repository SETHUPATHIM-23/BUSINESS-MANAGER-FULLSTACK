package com.businessmanager.backend.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class BalanceSheetDto {
    private LocalDate asOfDate;
    
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    
    private List<AccountLine> assetAccounts;
    private List<AccountLine> liabilityAccounts;
    private List<AccountLine> equityAccounts;

    @Data
    @Builder
    public static class AccountLine {
        private String accountCode;
        private String accountName;
        private BigDecimal balance;
    }
}
