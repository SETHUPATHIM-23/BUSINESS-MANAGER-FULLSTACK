package com.businessmanager.backend.customer.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CustomerStatementResponse {
    private String customerCode;
    private String customerName;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<StatementEntry> entries;
    
    // CUST-040 Reconciliation fields
    private BigDecimal totalUnpaidInvoices;
    private BigDecimal totalUnappliedCredits;
    private BigDecimal reconciledBalance;

    @Data
    @Builder
    public static class StatementEntry {
        private LocalDate date;
        private String documentCode;
        private String description;
        private String type; // INVOICE, PAYMENT, CREDIT_NOTE
        private BigDecimal amount;
    }
}
