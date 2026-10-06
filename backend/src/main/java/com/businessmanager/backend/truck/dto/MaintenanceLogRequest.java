package com.businessmanager.backend.truck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceLogRequest {

    @NotNull(message = "Truck ID is required")
    private Long truckId;

    @NotNull(message = "Maintenance date is required")
    private LocalDate date;

    @NotBlank(message = "Maintenance type is required")
    private String type;

    @NotNull(message = "Cost is required")
    @PositiveOrZero(message = "Cost cannot be negative")
    private BigDecimal cost;

    @PositiveOrZero(message = "Odometer reading cannot be negative")
    private Double odometerReading;
}
