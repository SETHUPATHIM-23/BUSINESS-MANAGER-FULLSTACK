package com.businessmanager.backend.truck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceLogResponseDto {

    private Long id;
    private Long truckId;
    private String truckRegistrationNumber;
    private LocalDate date;
    private String type;
    private BigDecimal cost;
    private Double odometerReading;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
