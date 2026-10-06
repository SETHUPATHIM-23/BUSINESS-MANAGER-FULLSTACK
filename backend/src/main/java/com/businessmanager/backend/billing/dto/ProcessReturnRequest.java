package com.businessmanager.backend.billing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class ProcessReturnRequest {
    
    @NotEmpty(message = "Return items cannot be empty")
    private Map<Long, BigDecimal> returnedQuantities; // Map of InvoiceLine ID -> Quantity Returned
    
}
