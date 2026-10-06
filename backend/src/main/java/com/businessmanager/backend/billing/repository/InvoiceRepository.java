package com.businessmanager.backend.billing.repository;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // ── Unique business-key lookup ──

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    // ── Filtered / paginated search (list-endpoint convention) ──

    @Query(value = "SELECT i FROM Invoice i " +
                   "LEFT JOIN FETCH i.customer c " +
                   "WHERE (:search IS NULL " +
                   "       OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
                   "       OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                   "AND (:customerId IS NULL OR c.id = :customerId) " +
                   "AND (:status IS NULL OR i.status = :status) " +
                   "AND (:dateFrom IS NULL OR i.invoiceDate >= :dateFrom) " +
                   "AND (:dateTo IS NULL OR i.invoiceDate <= :dateTo)",
           countQuery = "SELECT COUNT(i) FROM Invoice i " +
                        "LEFT JOIN i.customer c " +
                        "WHERE (:search IS NULL " +
                        "       OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "       OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        "AND (:customerId IS NULL OR c.id = :customerId) " +
                        "AND (:status IS NULL OR i.status = :status) " +
                        "AND (:dateFrom IS NULL OR i.invoiceDate >= :dateFrom) " +
                        "AND (:dateTo IS NULL OR i.invoiceDate <= :dateTo)")
    Page<Invoice> searchInvoices(
            @Param("search") String search,
            @Param("customerId") Long customerId,
            @Param("status") InvoiceStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );

    // ── Customer-scoped queries (statements / credit checks) ──

    List<Invoice> findByCustomerIdOrderByInvoiceDateDesc(Long customerId);

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.customer c " +
           "WHERE (:customerId IS NULL OR c.id = :customerId) " +
           "AND (:startDate IS NULL OR i.invoiceDate >= :startDate) " +
           "AND (:endDate IS NULL OR i.invoiceDate <= :endDate) " +
           "ORDER BY i.invoiceDate DESC, i.id DESC")
    List<Invoice> findForSalesReport(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Invoice> findByCustomerIdAndStatusIn(Long customerId, List<InvoiceStatus> statuses);

    /**
     * Total outstanding balance for a given customer (grand_total − amount_paid)
     * across non-cancelled, non-voided invoices.
     */
    @Query("SELECT COALESCE(SUM(i.grandTotal - i.amountPaid), 0) " +
           "FROM Invoice i " +
           "WHERE i.customer.id = :customerId " +
           "AND i.status NOT IN (com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED, " +
           "                     com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED)")
    BigDecimal calculateOutstandingBalanceByCustomerId(@Param("customerId") Long customerId);

    /**
     * Total revenue across all non-cancelled, non-voided invoices (for dashboard / valuation).
     */
    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) " +
           "FROM Invoice i " +
           "WHERE i.status NOT IN (com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED, " +
           "                     com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED)")
    BigDecimal calculateTotalRevenue();

    /**
     * Total unpaid balance across the whole system (for dashboard / valuation).
     */
    @Query("SELECT COALESCE(SUM(i.grandTotal - i.amountPaid), 0) " +
           "FROM Invoice i " +
           "WHERE i.status NOT IN (com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED, " +
           "                     com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED, " +
           "                     com.businessmanager.backend.billing.enums.InvoiceStatus.PAID)")
    BigDecimal calculateTotalOutstandingBalance();

    // ── Overdue detection ──

    /**
     * Find all invoices past their due date that are not yet PAID / CANCELLED / VOIDED.
     */
    @Query("SELECT i FROM Invoice i " +
           "WHERE i.dueDate < :today " +
           "AND i.status NOT IN (com.businessmanager.backend.billing.enums.InvoiceStatus.PAID, " +
           "                     com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED, " +
           "                     com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED)")
    List<Invoice> findOverdueInvoices(@Param("today") LocalDate today);

    // ── Aggregate counts by status ──

    long countByStatus(InvoiceStatus status);

    /**
     * Count invoices for a specific customer and status (e.g. to check if customer
     * has any open invoices before account deletion).
     */
    long countByCustomerIdAndStatusIn(Long customerId, List<InvoiceStatus> statuses);

    // ── Next invoice number generation helper ──

    @Query("SELECT i.invoiceNumber FROM Invoice i " +
           "WHERE i.invoiceNumber LIKE CONCAT(:prefix, '%') " +
           "ORDER BY i.invoiceNumber DESC " +
           "LIMIT 1")
    Optional<String> findLatestInvoiceNumberByPrefix(@Param("prefix") String prefix);

    // ── Dashboard KPI Helpers ──

    @Query("SELECT COALESCE(SUM(il.lineTotal), 0) FROM InvoiceLine il " +
           "WHERE il.invoice.invoiceDate >= :startDate AND il.invoice.invoiceDate <= :endDate " +
           "AND il.invoice.status NOT IN (com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED, " +
           "                              com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED) " +
           "AND il.product.includeInFinancialCalculations = true")
    BigDecimal calculateSalesTotalForDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT i FROM Invoice i WHERE i.grandTotal > i.amountPaid " +
           "AND i.status NOT IN (com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED, " +
           "com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED)")
    List<Invoice> findAllOutstandingInvoices();
}
