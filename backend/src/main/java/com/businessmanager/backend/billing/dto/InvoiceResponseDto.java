package com.businessmanager.backend.billing.dto;

import com.businessmanager.backend.billing.enums.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full response body returned for a single Invoice (BILL-010 / BILL-020).
 * Includes all header fields, status, computed totals, and the embedded line items.
 */
@Data
public class InvoiceResponseDto {

    private Long id;
    private String invoiceNumber;

    // Customer (summary only — avoids lazy-load cycles)
    private Long customerId;
    private String customerName;
    private String customerAddress;
    private String customerGSTIN;
    private String customerState;
    private String customerStateCode;

    private LocalDate invoiceDate;
    private java.time.LocalTime invoiceTime;
    private LocalDate dueDate;
    private InvoiceStatus status;

    private BigDecimal subtotal;
    private BigDecimal taxTotal;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;

    private BigDecimal amountOutstanding;

    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal roundOff;
    private BigDecimal gstPercentage;
    private BigDecimal cgstRate;
    private BigDecimal sgstRate;
    private BigDecimal igstRate;

    private String billType;
    private String vehicleNo;
    private String notes;
    private List<InvoiceLineResponseDto> lines;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
}
