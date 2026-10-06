package com.businessmanager.backend.purchasing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SupplierInvoiceRequest {
    @NotNull(message = "PO ID is required")
    private Long purchaseOrderId;
    
    @NotNull(message = "Supplier invoiced subtotal is required")
    @PositiveOrZero
    private BigDecimal supplierInvoicedSubtotal;
    
    @PositiveOrZero
    private BigDecimal freightAmount = BigDecimal.ZERO;
    
    @PositiveOrZero
    private BigDecimal dutiesAmount = BigDecimal.ZERO;
    
    private boolean forceApprove = false;

    private java.time.LocalDate invoiceDate;

    private String supplierInvoiceNumber;
}
