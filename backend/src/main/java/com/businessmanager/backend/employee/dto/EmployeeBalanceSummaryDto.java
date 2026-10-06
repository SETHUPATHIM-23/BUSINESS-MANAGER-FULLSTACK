package com.businessmanager.backend.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeBalanceSummaryDto {
    private Long employeeId;
    private String employeeCode;
    private String name;
    private String department;
    private String roleTitle;
    private BigDecimal totalWorkedAmount;
    private BigDecimal totalSettledAmount;
    private BigDecimal netOutstandingBalance;
}
