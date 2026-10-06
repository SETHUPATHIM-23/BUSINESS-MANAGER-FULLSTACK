package com.businessmanager.backend.employee.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponseDto {
    private Long id;
    private String employeeCode;
    private String name;
    private String contactDetails;
    private String department;
    private String roleTitle;
    private LocalDate joiningDate;
    private String status;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
}
