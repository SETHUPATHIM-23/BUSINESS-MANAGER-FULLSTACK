package com.businessmanager.backend.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCreateRequest {

    @NotBlank(message = "Employee code is required")
    @Size(max = 50, message = "Employee code must be less than 50 characters")
    private String employeeCode;

    @NotBlank(message = "Employee name is required")
    @Size(max = 100, message = "Employee name must be less than 100 characters")
    private String name;

    @Size(max = 255, message = "Contact details must be less than 255 characters")
    private String contactDetails;

    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department must be less than 100 characters")
    private String department;

    @NotBlank(message = "Role title is required")
    @Size(max = 100, message = "Role title must be less than 100 characters")
    private String roleTitle;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private String status;

    private Long locationId;
}
