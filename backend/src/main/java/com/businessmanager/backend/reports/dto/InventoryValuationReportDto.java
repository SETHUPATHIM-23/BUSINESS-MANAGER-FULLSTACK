package com.businessmanager.backend.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InventoryValuationReportDto {
    private LocalDateTime generatedAt;
    
    private BigDecimal totalCatalogValuation;
    private long activeProductsCount;
    
    private StatementReportDto.CompanyInfo ourCompany;
    private List<ValuationLineDto> lines;

    @Data
    @Builder
    public static class ValuationLineDto {
        private String sku;
        private String name;
        private String category;
        private BigDecimal stockOnHand;
        private BigDecimal costPrice;
        private BigDecimal totalValue;
    }
}
