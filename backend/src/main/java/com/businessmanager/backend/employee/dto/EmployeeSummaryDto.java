package com.businessmanager.backend.employee.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSummaryDto {
    private Long id;
    private String employeeCode;
    private String name;
    private String department;
    private String roleTitle;
    private String status;
    private LocalDate joiningDate;
    private String locationName;
}
