package com.businessmanager.backend.billing.dto;

import com.businessmanager.backend.billing.enums.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lightweight summary DTO used in paginated invoice list responses.
 * Contains no embedded lines to keep page payloads compact.
 */
@Data
public class InvoiceSummaryDto {

    private Long id;
    private String invoiceNumber;

    private Long customerId;
    private String customerName;

    private LocalDate invoiceDate;
    private java.time.LocalTime invoiceTime;
    private LocalDate dueDate;
    private InvoiceStatus status;

    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal amountOutstanding;

    private String billType;
    private String vehicleNo;
    private LocalDateTime createdAt;
    private String createdBy;
}
