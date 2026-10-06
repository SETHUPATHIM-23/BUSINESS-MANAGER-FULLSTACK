package com.businessmanager.backend.employee.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.fund.entity.FundAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee_settlements")
@Getter
@Setter
public class EmployeeSettlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_account_id")
    private FundAccount fundAccount;

    @Column(name = "payment_mode", nullable = false, length = 50)
    private String paymentMode = "CASH";

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(length = 500)
    private String notes;
}
