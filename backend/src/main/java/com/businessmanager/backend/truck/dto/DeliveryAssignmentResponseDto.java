package com.businessmanager.backend.truck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignmentResponseDto {

    private Long id;
    private Long truckId;
    private String truckRegistrationNumber;
    private Long invoiceId;
    private String invoiceNumber;
    private String customerName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
