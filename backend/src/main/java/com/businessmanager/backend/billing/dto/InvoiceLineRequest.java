package com.businessmanager.backend.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request payload for a single invoice line item (create or update).
 * Used nested inside {@link InvoiceCreateRequest} and {@link InvoiceUpdateRequest}.
 */
@Data
public class InvoiceLineRequest {

    /** ID of the product being sold. Required. */
    @NotNull(message = "Product ID is required for each invoice line")
    private Long productId;

    /** Optional override of the product's name shown on the invoice. */
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    /** Quantity sold. Must be strictly positive. */
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    /**
     * Override unit price. If null/zero, the product's base selling price is used
     * by the service layer.
     */
    @PositiveOrZero(message = "Unit price must be zero or positive")
    private BigDecimal unitPrice;

    /** Per-line monetary discount (not a percentage). Defaults to zero. */
    @PositiveOrZero(message = "Discount must be zero or positive")
    private BigDecimal discount;

    /**
     * Override tax amount. If null/zero, the tax is computed from the product's
     * tax rate by the service layer.
     */
    @PositiveOrZero(message = "Tax amount must be zero or positive")
    private BigDecimal taxAmount;
}
