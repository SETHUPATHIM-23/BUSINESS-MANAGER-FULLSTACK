package com.businessmanager.backend.purchasing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class GoodsReceiptRequest {
    @NotNull(message = "PO ID is required")
    private Long poId;
    
    @NotNull(message = "Received date is required")
    private LocalDate receivedDate;
    
    @NotEmpty(message = "At least one receipt line is required")
    @Valid
    private List<GoodsReceiptLineRequest> lines;

    @Data
    public static class GoodsReceiptLineRequest {
        @NotNull(message = "Purchase line ID is required")
        private Long purchaseLineId;
        
        @NotNull(message = "Received quantity is required")
        @PositiveOrZero(message = "Received quantity must be positive or zero")
        private BigDecimal receivedQty;
    }
}
