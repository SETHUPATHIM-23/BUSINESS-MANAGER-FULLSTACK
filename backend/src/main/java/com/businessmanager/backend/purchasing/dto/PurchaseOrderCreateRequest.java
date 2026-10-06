package com.businessmanager.backend.purchasing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderCreateRequest {
    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private String poNumber;

    private LocalDate orderDate;

    @Valid
    private List<PurchaseLineRequest> lines;
}
