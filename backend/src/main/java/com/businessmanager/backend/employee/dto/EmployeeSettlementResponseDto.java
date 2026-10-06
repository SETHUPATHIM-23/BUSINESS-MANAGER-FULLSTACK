package com.businessmanager.backend.employee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EmployeeSettlementResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate settlementDate;
    private BigDecimal amount;
    private Long fundAccountId;
    private String fundAccountName;
    private String paymentMode;
    private String referenceNo;
    private String notes;
    private LocalDateTime createdAt;
}
