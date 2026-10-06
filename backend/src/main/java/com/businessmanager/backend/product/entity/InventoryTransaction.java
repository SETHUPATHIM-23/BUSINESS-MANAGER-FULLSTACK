package com.businessmanager.backend.product.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
public class InventoryTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    @Column(name = "reference_type", nullable = false, length = 30)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "quantity_change", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityChange;

    @Column(name = "stock_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal stockAfter;

    @Column(length = 255)
    private String notes;
}
