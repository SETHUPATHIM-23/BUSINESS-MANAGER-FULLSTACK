package com.businessmanager.backend.billing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response body for a single invoice line item, embedded in {@link InvoiceResponseDto}.
 */
@Data
public class InvoiceLineResponseDto {

    private Long id;

    // Product (summary only)
    private Long productId;
    private String productSku;
    private String productName;
    private String hsnCode;

    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
