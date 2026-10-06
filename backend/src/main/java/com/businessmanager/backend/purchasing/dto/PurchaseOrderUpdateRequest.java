package com.businessmanager.backend.purchasing.dto;

import jakarta.validation.Valid;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderUpdateRequest {
    private Long supplierId;
    private LocalDate orderDate;

    @Valid
    private List<PurchaseLineRequest> lines;
}
