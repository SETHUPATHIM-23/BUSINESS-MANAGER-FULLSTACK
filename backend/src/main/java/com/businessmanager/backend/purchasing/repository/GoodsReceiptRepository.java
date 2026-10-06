package com.businessmanager.backend.purchasing.repository;

import com.businessmanager.backend.purchasing.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    List<GoodsReceipt> findByPurchaseOrderId(Long poId);

    List<GoodsReceipt> findByPurchaseLineId(Long purchaseLineId);

    /**
     * Calculate total quantity received for a specific PO line.
     */
    @Query("SELECT COALESCE(SUM(gr.receivedQty), 0) FROM GoodsReceipt gr WHERE gr.purchaseLine.id = :purchaseLineId")
    BigDecimal calculateTotalReceivedQtyForLine(@Param("purchaseLineId") Long purchaseLineId);
    
    /**
     * Calculate total quantity received for an entire PO.
     */
    @Query("SELECT COALESCE(SUM(gr.receivedQty), 0) FROM GoodsReceipt gr WHERE gr.purchaseOrder.id = :poId")
    BigDecimal calculateTotalReceivedQtyForPo(@Param("poId") Long poId);
}
