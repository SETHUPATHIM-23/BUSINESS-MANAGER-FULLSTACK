package com.businessmanager.backend.purchasing.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PurchaseLineResponseDto {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private BigDecimal orderedQty;
    private BigDecimal costPrice;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
}
