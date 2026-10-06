package com.businessmanager.backend.purchasing.dto;

import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderResponseDto {
    private Long id;
    private String poNumber;
    private Long supplierId;
    private String supplierName;
    private LocalDate orderDate;
    private PurchaseOrderStatus status;
    private BigDecimal totalAmount; 
    
    private List<PurchaseLineResponseDto> lines;
}
