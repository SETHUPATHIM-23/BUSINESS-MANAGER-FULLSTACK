package com.businessmanager.backend.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Request payload for updating an existing DRAFT Invoice.
 * Only DRAFT invoices may be edited; the service enforces this rule.
 *
 * <p>Fields left null are ignored (partial update semantics at the header level).
 * The lines collection is always replaced in full when provided.
 */
@Data
public class InvoiceUpdateRequest {

    /** Re-assign to a different customer (optional). */
    private Long customerId;

    /** Override invoice date (optional). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceDate;

    /** Override invoice time (optional). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private java.time.LocalTime invoiceTime;

    /** Override due date (optional). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    /** Replace notes (optional). */
    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    /**
     * Full replacement line list. At least one line is required when lines are provided.
     * If this field is omitted, existing lines are left unchanged.
     */
    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<InvoiceLineRequest> lines;

    /** Type of billing to determine tax calculation strategy. (optional) */
    @Pattern(regexp = "^(tax_exclusive|tax_inclusive|delivery_challan|CREDIT_NOTE)$", message = "Invalid bill type")
    private String billType;

    /** Vehicle number assigned to delivery. (optional) */
    @Size(max = 100, message = "Vehicle number must not exceed 100 characters")
    private String vehicleNo;

    /** Document-level GST percentage. (optional) */
    @PositiveOrZero(message = "GST percentage must be zero or positive")
    private java.math.BigDecimal gstPercentage;

    /** IGST amount. (optional) */
    @PositiveOrZero(message = "IGST amount must be zero or positive")
    private java.math.BigDecimal igst;

    @PositiveOrZero(message = "CGST rate must be zero or positive")
    private java.math.BigDecimal cgstRate;

    @PositiveOrZero(message = "SGST rate must be zero or positive")
    private java.math.BigDecimal sgstRate;

    @PositiveOrZero(message = "IGST rate must be zero or positive")
    private java.math.BigDecimal igstRate;
}
