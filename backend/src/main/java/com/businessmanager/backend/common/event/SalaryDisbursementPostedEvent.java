package com.businessmanager.backend.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class SalaryDisbursementPostedEvent {
    private final Long fundAccountId;
    private final Long employeeId;
    private final BigDecimal amount;
    private final LocalDate date;
}
