package com.businessmanager.backend.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class CustomerPaymentPostedEvent {
    private final Long fundAccountId;
    private final Long invoiceId;
    private final BigDecimal amount;
    private final LocalDate date;
}
