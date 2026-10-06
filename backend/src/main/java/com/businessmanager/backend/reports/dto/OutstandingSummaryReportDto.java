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
public class OutstandingSummaryReportDto {
    private String reportType; // CUSTOMER_OUTSTANDING or SUPPLIER_OUTSTANDING
    private CompanyInfo ourCompany;
    private LocalDate asOfDate;
    private LocalDateTime generatedAt;

    private BigDecimal totalOutstandingAmount;
    private int partnersWithDueCount;
    private int totalPartnersCount;

    private List<OutstandingLine> lines;

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
    public static class OutstandingLine {
        private Long partnerId;
        private String partnerCode;
        private String partnerName;
        private String phone;
        private String email;
        private BigDecimal totalBilledOrPurchased;
        private BigDecimal totalPaid;
        private BigDecimal outstandingBalance;
        private String status;
    }
}
