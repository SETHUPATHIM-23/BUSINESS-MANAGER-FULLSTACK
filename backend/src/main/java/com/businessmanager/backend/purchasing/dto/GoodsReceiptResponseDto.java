package com.businessmanager.backend.purchasing.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoodsReceiptResponseDto {
    private Long id;
    private Long purchaseLineId;
    private LocalDate receivedDate;
    private BigDecimal receivedQty;
}
