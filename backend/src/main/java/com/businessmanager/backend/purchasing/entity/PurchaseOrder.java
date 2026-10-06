package com.businessmanager.backend.purchasing.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
import com.businessmanager.backend.supplier.entity.Supplier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
public class PurchaseOrder extends BaseEntity {

    @Column(name = "po_number", nullable = false, unique = true, length = 50)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "amount_paid", precision = 15, scale = 2)
    private java.math.BigDecimal amountPaid = java.math.BigDecimal.ZERO;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseLine> lines = new ArrayList<>();

    public void addLine(PurchaseLine line) {
        lines.add(line);
        line.setPurchaseOrder(this);
    }

    @Transient
    public java.math.BigDecimal getTotalAmount() {
        if (lines == null || lines.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        return lines.stream()
                .map(PurchaseLine::getLineTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}
