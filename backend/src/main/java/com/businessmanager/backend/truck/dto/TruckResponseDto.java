package com.businessmanager.backend.truck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TruckResponseDto {

    private Long id;
    private String registrationNumber;
    private String make;
    private String model;
    private Double capacity;
    private String fuelType;
    private Long driverEmployeeId;
    private String driverName;
    private String driverCode;
    private LocalDate lastServiceDate;
    private Boolean maintenanceDue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
