package com.businessmanager.backend.product.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "product_batches", uniqueConstraints = {
    @UniqueConstraint(name = "uk_product_batch", columnNames = {"product_id", "batch_number"})
})
@Getter
@Setter
public class ProductBatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "stock_on_hand", nullable = false, precision = 15, scale = 2)
    private BigDecimal stockOnHand = BigDecimal.ZERO;
}
