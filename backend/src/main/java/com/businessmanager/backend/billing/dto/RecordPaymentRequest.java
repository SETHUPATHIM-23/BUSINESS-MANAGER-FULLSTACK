package com.businessmanager.backend.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/billings/{id}/payments} and {@code /post-and-pay}.
 * Records a partial or full payment against an invoice.
 */
@Data
public class RecordPaymentRequest {

    private Long paymentAccountId;

    @JsonProperty("paymentAmount")
    private BigDecimal paymentAmount;

    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount != null ? amount : paymentAmount;
    }
}

