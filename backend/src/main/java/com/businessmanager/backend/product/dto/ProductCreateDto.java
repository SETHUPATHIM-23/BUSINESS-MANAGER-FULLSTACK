package com.businessmanager.backend.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateDto {

    @NotBlank(message = "SKU code is required")
    @Size(max = 50, message = "SKU code must not exceed 50 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name must not exceed 100 characters")
    private String name;

    private String hsnCode;

    private String description;

    private Long categoryId;

    @NotBlank(message = "Unit of measure is required")
    @Size(max = 20, message = "Unit of measure must not exceed 20 characters")
    private String unitOfMeasure;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.00", message = "Cost price must be zero or positive")
    private BigDecimal costPrice;

    @NotNull(message = "Base selling price is required")
    @DecimalMin(value = "0.00", message = "Base selling price must be zero or positive")
    private BigDecimal baseSellingPrice;

    private Long taxRateId;

    @NotNull(message = "Reorder level is required")
    @DecimalMin(value = "0.00", message = "Reorder level must be zero or positive")
    private BigDecimal reorderLevel;

    @NotNull(message = "Reorder quantity is required")
    @DecimalMin(value = "0.00", message = "Reorder quantity must be zero or positive")
    private BigDecimal reorderQty;

    private boolean batchTracked;

    private boolean includeInFinancialCalculations = true;
}
