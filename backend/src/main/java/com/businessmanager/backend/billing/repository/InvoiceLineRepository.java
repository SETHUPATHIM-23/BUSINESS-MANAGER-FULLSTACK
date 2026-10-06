package com.businessmanager.backend.billing.repository;

import com.businessmanager.backend.billing.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    // ── Lines by invoice ──

    List<InvoiceLine> findByInvoiceIdOrderByIdAsc(Long invoiceId);

    long countByInvoiceId(Long invoiceId);

    // ── Product-scoped queries ──

    /**
     * Check if any invoice line references a given product
     * (used to prevent product deletion when transaction history exists).
     */
    boolean existsByProductId(Long productId);

    /**
     * Total quantity sold for a specific product across all invoice lines.
     */
    @Query("SELECT COALESCE(SUM(il.quantity), 0) FROM InvoiceLine il " +
           "WHERE il.product.id = :productId " +
           "AND il.invoice.status NOT IN " +
           "(com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED, " +
           " com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED)")
    BigDecimal calculateTotalQuantitySoldByProductId(@Param("productId") Long productId);

    /**
     * Total revenue for a specific product across all invoice lines.
     */
    @Query("SELECT COALESCE(SUM(il.lineTotal), 0) FROM InvoiceLine il " +
           "WHERE il.product.id = :productId " +
           "AND il.invoice.status NOT IN " +
           "(com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED, " +
           " com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED)")
    BigDecimal calculateTotalRevenueByProductId(@Param("productId") Long productId);

    // ── Recalculation helper ──

    /**
     * Sum of all line totals for a given invoice (for subtotal recomputation).
     */
    @Query("SELECT COALESCE(SUM(il.lineTotal), 0) FROM InvoiceLine il WHERE il.invoice.id = :invoiceId")
    BigDecimal sumLineTotalsByInvoiceId(@Param("invoiceId") Long invoiceId);

    /**
     * Sum of all tax amounts for a given invoice (for tax total recomputation).
     */
    @Query("SELECT COALESCE(SUM(il.taxAmount), 0) FROM InvoiceLine il WHERE il.invoice.id = :invoiceId")
    BigDecimal sumTaxAmountsByInvoiceId(@Param("invoiceId") Long invoiceId);
}
