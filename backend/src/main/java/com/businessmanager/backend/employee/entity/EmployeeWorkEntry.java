package com.businessmanager.backend.employee.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee_work_entries")
@Getter
@Setter
public class EmployeeWorkEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "worked_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal workedAmount;

    @Column(length = 500)
    private String description;

    @Column(name = "is_settled", nullable = false)
    private Boolean isSettled = false;
}
