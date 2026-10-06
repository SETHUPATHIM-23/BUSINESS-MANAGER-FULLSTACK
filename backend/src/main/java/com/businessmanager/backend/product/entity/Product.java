package com.businessmanager.backend.product.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.product.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "hsn_code", length = 50)
    private String hsnCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(name = "cost_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "base_selling_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseSellingPrice = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_rate_id")
    private TaxRate taxRate;

    @Column(name = "reorder_level", nullable = false, precision = 15, scale = 2)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "reorder_qty", nullable = false, precision = 15, scale = 2)
    private BigDecimal reorderQty = BigDecimal.ZERO;

    @Column(name = "batch_tracked", nullable = false)
    private boolean batchTracked = false;

    @Column(name = "include_in_financial_calculations", nullable = false)
    private boolean includeInFinancialCalculations = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "stock_on_hand", nullable = false, precision = 15, scale = 2)
    private BigDecimal stockOnHand = BigDecimal.ZERO;

    @Column(name = "allow_negative_stock", nullable = false)
    private boolean allowNegativeStock = true;
}
