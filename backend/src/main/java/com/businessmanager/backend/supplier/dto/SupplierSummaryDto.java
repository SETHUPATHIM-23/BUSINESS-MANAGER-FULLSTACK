package com.businessmanager.backend.supplier.dto;

import com.businessmanager.backend.supplier.enums.SupplierStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SupplierSummaryDto {
    private Long id;
    private String supplierCode;
    private String name;
    private String businessName;
    private String phone;
    private String email;
    private int paymentTermsDays;
    private SupplierStatus status;
}
