package com.businessmanager.backend.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrialBalanceItem {
        private Long accountId;
        private String code;
        private String name;
        private String type;
        private BigDecimal debitTotal;
        private BigDecimal creditTotal;
        private BigDecimal netBalance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeneralLedgerLine {
        private LocalDate entryDate;
        private String reference;
        private String memo;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private BigDecimal runningBalance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeneralLedgerDetail {
        private Long accountId;
        private String code;
        private String name;
        private String type;
        private BigDecimal openingBalance;
        private List<GeneralLedgerLine> lines;
        private BigDecimal closingBalance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportAccountItem {
        private String code;
        private String name;
        private BigDecimal balance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProfitAndLossReport {
        private List<ReportAccountItem> revenues;
        private BigDecimal totalRevenue;
        private List<ReportAccountItem> expenses;
        private BigDecimal totalExpenses;
        private BigDecimal netIncome;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BalanceSheetReport {
        private List<ReportAccountItem> assets;
        private BigDecimal totalAssets;
        private List<ReportAccountItem> liabilities;
        private BigDecimal totalLiabilities;
        private List<ReportAccountItem> equity;
        private BigDecimal totalEquity;
        private BigDecimal totalLiabilitiesAndEquity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaxSummaryReport {
        private BigDecimal taxCollected; // Tax Payable (Output Tax) credit sum
        private BigDecimal taxPaid; // Tax Receivable (Input Tax) debit sum
        private BigDecimal netTaxPayable; // Collected - Paid
        private List<GeneralLedgerLine> collectedDetails;
        private List<GeneralLedgerLine> paidDetails;
    }
}
