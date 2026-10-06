package com.businessmanager.backend.customer.dto;

import com.businessmanager.backend.customer.enums.CustomerStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerResponseDto {
    private Long id;
    private String customerCode;
    private String name;
    private String businessName;
    private String phone;
    private String email;
    private String address;
    private String taxId;
    private BigDecimal creditLimit;
    private boolean creditHold;
    private Long priceTierId;
    private BigDecimal openingBalance;
    private CustomerStatus status;
    
    // Auditing fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
}
