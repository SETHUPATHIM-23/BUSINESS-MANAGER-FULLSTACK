package com.businessmanager.backend.supplier.entity;

import com.businessmanager.backend.common.crypto.EncryptionConverter;
import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.supplier.enums.SupplierStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
public class Supplier extends BaseEntity {

    @Column(name = "supplier_code", nullable = false, unique = true, length = 50)
    private String supplierCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "business_name", length = 100)
    private String businessName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "payment_terms_days", nullable = false)
    private int paymentTermsDays = 0;

    @Column(name = "bank_account_details", length = 500)
    @Convert(converter = EncryptionConverter.class)
    private String bankAccountDetails;

    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "running_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal runningBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierStatus status = SupplierStatus.ACTIVE;
}
