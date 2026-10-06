package com.businessmanager.backend.employee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EmployeeWorkEntryResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate date;
    private BigDecimal workedAmount;
    private String description;
    private Boolean isSettled;
    private LocalDateTime createdAt;
}
