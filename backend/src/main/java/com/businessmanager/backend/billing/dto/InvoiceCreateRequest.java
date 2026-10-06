package com.businessmanager.backend.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Request payload for creating a new Invoice (BILL-010).
 *
 * <p>The service layer will:
 * <ul>
 *   <li>auto-generate the invoice number if not supplied</li>
 *   <li>default invoiceDate to today and dueDate to today + 30 days</li>
 *   <li>force initial status to DRAFT</li>
 *   <li>resolve product references and compute per-line and header totals</li>
 * </ul>
 */
@Data
public class InvoiceCreateRequest {

    /** Customer to bill. Required. */
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    /**
     * Optional caller-supplied invoice number. If blank, one is auto-generated
     * with prefix "INV-".
     */
    @Size(max = 50, message = "Invoice number must not exceed 50 characters")
    private String invoiceNumber;

    /** Invoice date. Defaults to today if not supplied. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceDate;

    /** Invoice time. Optional. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private java.time.LocalTime invoiceTime;

    /** Payment due date. Defaults to invoiceDate + 30 days if not supplied. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    /** Optional free-text notes printed on the invoice. */
    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    /** At least one line item is required. */
    @NotEmpty(message = "An invoice must have at least one line item")
    @Valid
    private List<InvoiceLineRequest> lines;

    /** Type of billing to determine tax calculation strategy. */
    @NotNull(message = "Bill type is required")
    @Pattern(regexp = "^(tax_exclusive|tax_inclusive|delivery_challan|CREDIT_NOTE)$", message = "Invalid bill type")
    private String billType;

    /** Vehicle number assigned to delivery. Optional. */
    @Size(max = 100, message = "Vehicle number must not exceed 100 characters")
    private String vehicleNo;

    /** Document-level GST percentage. */
    @PositiveOrZero(message = "GST percentage must be zero or positive")
    private java.math.BigDecimal gstPercentage;

    /** IGST amount. */
    @PositiveOrZero(message = "IGST amount must be zero or positive")
    private java.math.BigDecimal igst;

    @PositiveOrZero(message = "CGST rate must be zero or positive")
    private java.math.BigDecimal cgstRate;

    @PositiveOrZero(message = "SGST rate must be zero or positive")
    private java.math.BigDecimal sgstRate;

    @PositiveOrZero(message = "IGST rate must be zero or positive")
    private java.math.BigDecimal igstRate;
}
