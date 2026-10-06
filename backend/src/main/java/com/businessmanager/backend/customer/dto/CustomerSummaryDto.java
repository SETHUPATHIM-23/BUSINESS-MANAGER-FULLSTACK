package com.businessmanager.backend.customer.dto;

import com.businessmanager.backend.customer.enums.CustomerStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerSummaryDto {
    private Long id;
    private String customerCode;
    private String name;
    private String businessName;
    private String phone;
    private String email;
    private String address;
    private String taxId;
    private String state;
    private String stateCode;
    private BigDecimal creditLimit;
    private boolean creditHold;
    private CustomerStatus status;
}
