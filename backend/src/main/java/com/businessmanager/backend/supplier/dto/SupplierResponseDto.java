package com.businessmanager.backend.supplier.dto;

import com.businessmanager.backend.supplier.enums.SupplierStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SupplierResponseDto {
    private Long id;
    private String supplierCode;
    private String name;
    private String businessName;
    private String phone;
    private String email;
    private String address;
    private String taxId;
    private int paymentTermsDays;
    private String bankAccountDetails;
    private BigDecimal openingBalance;
    private BigDecimal runningBalance;
    private SupplierStatus status;
    
    // Auditing fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
}
