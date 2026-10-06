package com.businessmanager.backend.supplier.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SupplierStatementResponse {
    private String supplierCode;
    private String supplierName;
    private String bankAccountDetails; // Decrypted bank details
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<StatementEntry> entries;

    // Reconciliation check parameters (SUPP-030/SUPP-040)
    private BigDecimal totalUnpaidInvoices;
    private BigDecimal totalUnappliedDebits;
    private BigDecimal reconciledBalance;

    @Data
    @Builder
    public static class StatementEntry {
        private LocalDate date;
        private String documentCode;
        private String description;
        private String type; // PURCHASE_INVOICE, PAYMENT, DEBIT_NOTE
        private BigDecimal amount;
    }
}
