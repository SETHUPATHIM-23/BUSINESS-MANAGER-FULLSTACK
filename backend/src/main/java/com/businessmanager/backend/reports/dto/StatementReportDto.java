package com.businessmanager.backend.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementReportDto {
    private String statementType; // CUSTOMER or SUPPLIER
    private CompanyInfo ourCompany;
    private PartnerInfo partner;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime generatedAt;

    private BigDecimal openingBalance;
    private BigDecimal totalBilledOrPurchased;
    private BigDecimal totalPaidOrSettled;
    private BigDecimal closingBalance;

    private List<StatementLine> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyInfo {
        private String name;
        private String address;
        private String phone;
        private String email;
        private String taxId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartnerInfo {
        private Long id;
        private String code;
        private String name;
        private String phone;
        private String email;
        private String address;
        private String taxIdOrBank;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatementLine {
        private LocalDate date;
        private String documentCode;
        private String description;
        private String type; // SALE, PURCHASE, PAYMENT
        private BigDecimal billedOrPurchasedAmount;
        private BigDecimal paidAmount;
        private BigDecimal runningBalance;
    }
}
