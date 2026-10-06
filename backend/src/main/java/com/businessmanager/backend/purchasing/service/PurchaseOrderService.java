package com.businessmanager.backend.purchasing.service;

import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PurchaseOrderService {
    
    PurchaseOrder createPurchaseOrder(PurchaseOrder purchaseOrder);
    
    PurchaseOrder getPurchaseOrderById(Long id);
    
    PurchaseOrder getPurchaseOrderByPoNumber(String poNumber);
    
    Page<PurchaseOrder> searchPurchaseOrders(
            String search, 
            Long supplierId, 
            PurchaseOrderStatus status, 
            LocalDate dateFrom, 
            LocalDate dateTo, 
            Pageable pageable
    );
    
    PurchaseOrder updatePurchaseOrder(Long id, PurchaseOrder updated);
    
    PurchaseOrder changeStatus(Long id, PurchaseOrderStatus newStatus);
    
    void cancelPurchaseOrder(Long id);
    
    void deletePurchaseOrder(Long id);
    
    void recordSupplierPayment(Long id, Long paymentAccountId, java.math.BigDecimal amount);
    
    String generateNextPoNumber(String prefix);
}
