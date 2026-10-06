package com.businessmanager.backend.purchasing.repository;

import com.businessmanager.backend.purchasing.entity.PurchaseLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PurchaseLineRepository extends JpaRepository<PurchaseLine, Long> {

    List<PurchaseLine> findByPurchaseOrderIdOrderByIdAsc(Long poId);

    List<PurchaseLine> findByProductId(Long productId);

    /**
     * Total expected cost/valuation of open (non-cancelled) POs
     * for a specific supplier.
     */
    @Query("SELECT COALESCE(SUM(pl.lineTotal), 0) FROM PurchaseLine pl " +
           "WHERE pl.purchaseOrder.supplier.id = :supplierId " +
           "AND pl.purchaseOrder.status NOT IN (com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.CANCELLED)")
    BigDecimal calculateTotalExpectedCostBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * Total valuation of all active purchase orders in the system.
     */
    @Query("SELECT COALESCE(SUM(pl.lineTotal), 0) FROM PurchaseLine pl " +
           "WHERE pl.purchaseOrder.status NOT IN (com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.CANCELLED)")
    BigDecimal calculateTotalActivePoValuation();
}
