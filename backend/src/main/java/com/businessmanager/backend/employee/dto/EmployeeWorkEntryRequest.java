package com.businessmanager.backend.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeWorkEntryRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Worked amount is required")
    @DecimalMin(value = "0.01", message = "Worked amount must be greater than 0")
    private BigDecimal workedAmount;

    private String description;
}
