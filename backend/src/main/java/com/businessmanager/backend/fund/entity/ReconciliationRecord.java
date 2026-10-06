package com.businessmanager.backend.fund.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_records")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_transaction_id", nullable = false)
    private FundTransaction fundTransaction;

    @Column(name = "bank_statement_line_ref", nullable = false, length = 100)
    private String bankStatementLineRef;

    @Column(nullable = false)
    private Boolean matched = false;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "reconciled_by", length = 100)
    private String reconciledBy;
}
