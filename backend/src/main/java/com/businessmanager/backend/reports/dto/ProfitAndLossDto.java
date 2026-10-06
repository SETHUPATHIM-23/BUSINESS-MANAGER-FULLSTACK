package com.businessmanager.backend.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProfitAndLossDto {
    private LocalDate startDate;
    private LocalDate endDate;
    
    private BigDecimal totalRevenue;
    private BigDecimal totalCostOfGoodsSold;
    private BigDecimal grossProfit;
    private BigDecimal totalExpenses;
    private BigDecimal netIncome;

    private List<AccountLine> revenueAccounts;
    private List<AccountLine> cogsAccounts;
    private List<AccountLine> expenseAccounts;

    @Data
    @Builder
    public static class AccountLine {
        private String accountCode;
        private String accountName;
        private BigDecimal balance;
    }
}
