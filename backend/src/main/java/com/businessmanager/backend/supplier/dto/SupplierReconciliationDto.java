package com.businessmanager.backend.supplier.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class SupplierReconciliationDto {
    private String supplierCode;
    private String supplierName;
    private BigDecimal storedBalance;
    private BigDecimal recomputedBalance;
    private BigDecimal drift;
    private boolean reconciled;
}
