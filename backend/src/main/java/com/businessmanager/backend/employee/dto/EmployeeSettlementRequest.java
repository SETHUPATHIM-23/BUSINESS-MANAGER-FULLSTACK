package com.businessmanager.backend.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeSettlementRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Settlement date is required")
    private LocalDate settlementDate;

    @NotNull(message = "Settlement amount is required")
    @DecimalMin(value = "0.01", message = "Settlement amount must be greater than 0")
    private BigDecimal amount;

    private Long fundAccountId;

    private String paymentMode = "CASH";

    private String referenceNo;

    private String notes;
}
