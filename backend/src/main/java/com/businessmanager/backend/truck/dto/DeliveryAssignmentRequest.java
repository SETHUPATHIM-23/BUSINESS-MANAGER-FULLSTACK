package com.businessmanager.backend.truck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignmentRequest {

    @NotNull(message = "Truck ID is required")
    private Long truckId;

    private Long invoiceId;

    @NotBlank(message = "Delivery status is required")
    private String status;
}
