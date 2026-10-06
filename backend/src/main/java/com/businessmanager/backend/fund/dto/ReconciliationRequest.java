package com.businessmanager.backend.fund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRequest {

    @NotNull(message = "Fund transaction ID is required")
    private Long fundTransactionId;

    @NotBlank(message = "Bank statement line reference is required")
    @Size(max = 100, message = "Reference must not exceed 100 characters")
    private String bankStatementLineRef;

    @NotNull(message = "Matched status is required")
    private Boolean matched;
}
