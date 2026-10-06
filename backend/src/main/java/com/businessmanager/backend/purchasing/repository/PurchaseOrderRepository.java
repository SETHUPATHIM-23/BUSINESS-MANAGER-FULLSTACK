package com.businessmanager.backend.purchasing.repository;

import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
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
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // ── Unique business-key lookup ──
    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    boolean existsByPoNumber(String poNumber);

    // ── Filtered / paginated search (list-endpoint convention) ──
    @Query(value = "SELECT po FROM PurchaseOrder po " +
                   "LEFT JOIN FETCH po.supplier s " +
                   "WHERE (:search IS NULL " +
                   "       OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
                   "       OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                   "AND (:supplierId IS NULL OR s.id = :supplierId) " +
                   "AND (:status IS NULL OR po.status = :status) " +
                   "AND (:dateFrom IS NULL OR po.orderDate >= :dateFrom) " +
                   "AND (:dateTo IS NULL OR po.orderDate <= :dateTo)",
           countQuery = "SELECT COUNT(po) FROM PurchaseOrder po " +
                        "LEFT JOIN po.supplier s " +
                        "WHERE (:search IS NULL " +
                        "       OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "       OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        "AND (:supplierId IS NULL OR s.id = :supplierId) " +
                        "AND (:status IS NULL OR po.status = :status) " +
                        "AND (:dateFrom IS NULL OR po.orderDate >= :dateFrom) " +
                        "AND (:dateTo IS NULL OR po.orderDate <= :dateTo)")
    Page<PurchaseOrder> searchPurchaseOrders(
            @Param("search") String search,
            @Param("supplierId") Long supplierId,
            @Param("status") PurchaseOrderStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );

    // ── Supplier-scoped queries ──
    List<PurchaseOrder> findBySupplierIdOrderByOrderDateDesc(Long supplierId);

    @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.supplier s " +
           "WHERE (:supplierId IS NULL OR s.id = :supplierId) " +
           "AND (:startDate IS NULL OR po.orderDate >= :startDate) " +
           "AND (:endDate IS NULL OR po.orderDate <= :endDate) " +
           "ORDER BY po.orderDate DESC, po.id DESC")
    List<PurchaseOrder> findForPurchasesReport(
            @Param("supplierId") Long supplierId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT po.poNumber FROM PurchaseOrder po " +
           "WHERE po.poNumber LIKE CONCAT(:prefix, '%') " +
           "ORDER BY po.poNumber DESC " +
           "LIMIT 1")
    Optional<String> findLatestPoNumberByPrefix(@Param("prefix") String prefix);

    // ── Dashboard KPI Helpers ──

    @Query("SELECT COALESCE(SUM(l.lineTotal), 0) FROM PurchaseLine l " +
           "WHERE l.purchaseOrder.orderDate >= :startDate AND l.purchaseOrder.orderDate <= :endDate " +
           "AND l.purchaseOrder.status != com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.CANCELLED " +
           "AND l.product.includeInFinancialCalculations = true")
    BigDecimal calculatePurchasesTotalForDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ── Dashboard Payables Aging (DASH-060) ──
    // Returns all purchase orders that have been received (supplier invoice posted, creating AP liability).
    // The effective due date is computed in the service as: po.orderDate + po.supplier.paymentTermsDays.
    // We project the PO's line total sum alongside so the service can bucket the outstanding amount.
    @Query("SELECT po FROM PurchaseOrder po " +
           "JOIN FETCH po.supplier " +
           "WHERE po.status IN (" +
           "  com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.RECEIVED, " +
           "  com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.PARTIALLY_RECEIVED)")
    List<PurchaseOrder> findAllOutstandingPurchaseOrders();

    // Returns the total invoiced amount for a given PO (sum of line totals)
    @Query("SELECT COALESCE(SUM(l.lineTotal), 0) FROM PurchaseLine l WHERE l.purchaseOrder.id = :poId")
    BigDecimal sumLineTotalsByPoId(@Param("poId") Long poId);
}
