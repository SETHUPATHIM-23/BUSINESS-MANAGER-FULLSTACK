package com.businessmanager.backend.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SalesReportDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long customerId;
    
    private BigDecimal totalRevenue;
    private long invoiceCount;
    
    private StatementReportDto.CompanyInfo ourCompany;
    private List<SalesLineDto> lines;

    @Data
    @Builder
    public static class SalesLineDto {
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private String customerName;
        private BigDecimal subTotal;
        private BigDecimal taxTotal;
        private BigDecimal grandTotal;
    }
}
