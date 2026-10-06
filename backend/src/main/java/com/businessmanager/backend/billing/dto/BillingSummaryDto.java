package com.businessmanager.backend.billing.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Response body for dashboard / special-view aggregate endpoints:
 * <ul>
 *   <li>{@code GET /api/billings/summary/revenue}</li>
 *   <li>{@code GET /api/billings/summary/outstanding}</li>
 *   <li>{@code GET /api/billings/customers/{customerId}/outstanding}</li>
 * </ul>
 */
@Data
public class BillingSummaryDto {

    /** Label describing what the value represents. */
    private String label;

    /** The monetary figure. */
    private BigDecimal amount;

    public static BillingSummaryDto of(String label, BigDecimal amount) {
        BillingSummaryDto dto = new BillingSummaryDto();
        dto.label = label;
        dto.amount = amount != null ? amount : BigDecimal.ZERO;
        return dto;
    }
}
