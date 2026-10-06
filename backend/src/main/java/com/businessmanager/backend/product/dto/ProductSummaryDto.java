package com.businessmanager.backend.product.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSummaryDto {
    private Long id;
    private String sku;
    private String name;
    private String hsnCode;
    private String categoryName;
    private String unitOfMeasure;
    private BigDecimal costPrice;
    private BigDecimal baseSellingPrice;
    private String status;
}
