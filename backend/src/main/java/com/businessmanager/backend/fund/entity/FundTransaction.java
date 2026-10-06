package com.businessmanager.backend.fund.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fund_transactions")
@Getter
@Setter
@NoArgsConstructor
public class FundTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_account_id", nullable = false)
    private FundAccount fundAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundTransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "reference_document_type", length = 50)
    private String referenceDocumentType;

    @Column(name = "reference_document_id")
    private Long referenceDocumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_fund_account_id")
    private FundAccount targetFundAccount;

    @Column(length = 255)
    private String description;
}
