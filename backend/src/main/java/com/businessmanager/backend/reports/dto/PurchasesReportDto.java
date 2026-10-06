package com.businessmanager.backend.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PurchasesReportDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long supplierId;
    
    private BigDecimal totalPurchases;
    private long poCount;
    
    private StatementReportDto.CompanyInfo ourCompany;
    private List<PurchaseLineDto> lines;

    @Data
    @Builder
    public static class PurchaseLineDto {
        private String poNumber;
        private LocalDate orderDate;
        private String supplierName;
        private BigDecimal totalAmount;
        private String status;
    }
}
