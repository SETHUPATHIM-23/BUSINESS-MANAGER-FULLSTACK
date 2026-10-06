package com.businessmanager.backend.product.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductDetailDto {
    private Long id;
    private String sku;
    private String name;
    private String hsnCode;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String unitOfMeasure;
    private BigDecimal costPrice;
    private BigDecimal baseSellingPrice;
    private Long taxRateId;
    private String taxRateName;
    private BigDecimal taxRatePercent;
    private BigDecimal reorderLevel;
    private BigDecimal reorderQty;
    private boolean batchTracked;
    private boolean includeInFinancialCalculations;
    private String status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
