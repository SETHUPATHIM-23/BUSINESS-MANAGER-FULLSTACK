package com.businessmanager.backend.truck.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class TruckExpensePostingRequest {

    @NotNull(message = "Truck ID is required")
    private Long truckId;

    private Long maintenanceLogId;

    @NotNull(message = "Expense ledger account ID is required")
    private Long expenseAccountId;

    @NotNull(message = "Payment source ledger account ID is required")
    private Long paymentAccountId;

    @NotNull(message = "Expense amount is required")
    @Positive(message = "Expense amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Posting date is required")
    private LocalDate date;

    @NotNull(message = "Expense type is required")
    private String expenseType; // e.g. FUEL, MAINTENANCE, REPAIR

    private String memo;
}
