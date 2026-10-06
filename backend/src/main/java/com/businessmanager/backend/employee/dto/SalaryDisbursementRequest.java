package com.businessmanager.backend.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryDisbursementRequest {

    @NotNull(message = "Payment source account ID is required")
    private Long paymentAccountId;

    @NotNull(message = "Disbursement date is required")
    private LocalDate disbursementDate;

    private String memo;
}
