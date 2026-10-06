package com.businessmanager.backend.billing.entity;

import com.businessmanager.backend.billing.enums.InvoiceStatus;
import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "invoice_time")
    private LocalTime invoiceTime;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal cgst = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal sgst = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal igst = BigDecimal.ZERO;

    @Column(name = "round_off", precision = 15, scale = 2)
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage = new BigDecimal("18.00");

    @Column(name = "cgst_rate", precision = 5, scale = 2)
    private BigDecimal cgstRate = BigDecimal.ZERO;

    @Column(name = "sgst_rate", precision = 5, scale = 2)
    private BigDecimal sgstRate = BigDecimal.ZERO;

    @Column(name = "igst_rate", precision = 5, scale = 2)
    private BigDecimal igstRate = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "vehicle_no", length = 100)
    private String vehicleNo;

    @Column(name = "bill_type", length = 30)
    private String billType;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLine> lines = new ArrayList<>();

    // ── Convenience helpers (no business logic) ──

    /**
     * Add a line to this invoice (maintains bidirectional relationship).
     */
    public void addLine(InvoiceLine line) {
        lines.add(line);
        line.setInvoice(this);
    }

    /**
     * Remove a line from this invoice (maintains bidirectional relationship).
     */
    public void removeLine(InvoiceLine line) {
        lines.remove(line);
        line.setInvoice(null);
    }
}
