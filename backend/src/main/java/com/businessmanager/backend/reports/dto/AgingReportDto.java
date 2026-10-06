package com.businessmanager.backend.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AgingReportDto {
    private LocalDate asOfDate;
    private String type; // "RECEIVABLES" or "PAYABLES"
    
    private BigDecimal totalCurrent;
    private BigDecimal total1to30;
    private BigDecimal total31to60;
    private BigDecimal total61to90;
    private BigDecimal totalOver90;
    private BigDecimal grandTotal;
    
    private List<AgingLine> lines;

    @Data
    @Builder
    public static class AgingLine {
        private String partnerName; // Customer or Supplier Name
        private String documentNumber; // Invoice or PO Number
        private LocalDate dueDate;
        private BigDecimal currentAmount;
        private BigDecimal days1to30Amount;
        private BigDecimal days31to60Amount;
        private BigDecimal days61to90Amount;
        private BigDecimal over90Amount;
        private BigDecimal totalAmount;
    }
}
