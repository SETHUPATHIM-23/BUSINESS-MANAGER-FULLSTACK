package com.businessmanager.backend.purchasing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PurchaseLineRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Ordered quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be strictly positive")
    private BigDecimal orderedQty;

    @DecimalMin(value = "0.00", message = "Cost price must be positive or zero")
    private BigDecimal costPrice;

    @DecimalMin(value = "0.00", message = "Tax amount must be positive or zero")
    private BigDecimal taxAmount;
}
