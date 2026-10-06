package com.businessmanager.backend.purchasing.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.purchasing.entity.PurchaseLine;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
import com.businessmanager.backend.purchasing.repository.GoodsReceiptRepository;
import com.businessmanager.backend.purchasing.repository.PurchaseLineRepository;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import com.businessmanager.backend.supplier.entity.Supplier;
import com.businessmanager.backend.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PurchaseOrderServiceImplTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseLineRepository purchaseLineRepository;
    @Mock private GoodsReceiptRepository goodsReceiptRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private PurchaseOrderServiceImpl service;

    private Supplier supplier;
    private Product product;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Test Supplier");

        product = new Product();
        product.setId(10L);
        product.setSku("SKU-1");
        product.setCostPrice(new BigDecimal("50.00"));
    }

    @Test
    void createPurchaseOrder_Success() {
        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        PurchaseLine line = new PurchaseLine();
        line.setProduct(product);
        line.setOrderedQty(new BigDecimal("10"));
        po.addLine(line);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(purchaseOrderRepository.existsByPoNumber(anyString())).thenReturn(false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(i -> {
            PurchaseOrder saved = i.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        PurchaseOrder savedPo = service.createPurchaseOrder(po);

        assertNotNull(savedPo.getId());
        assertEquals(PurchaseOrderStatus.DRAFT, savedPo.getStatus());
        assertTrue(savedPo.getPoNumber().startsWith("PO-"));
        assertEquals(new BigDecimal("500.00"), savedPo.getLines().get(0).getLineTotal());
    }

    @Test
    void createPurchaseOrder_UniqueConstraintViolation() {
        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setPoNumber("PO-000001");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(purchaseOrderRepository.existsByPoNumber("PO-000001")).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> service.createPurchaseOrder(po));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void deletePurchaseOrder_DraftWithoutReceipts_Success() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(1L);
        po.setStatus(PurchaseOrderStatus.DRAFT);

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(po));
        when(goodsReceiptRepository.calculateTotalReceivedQtyForPo(1L)).thenReturn(BigDecimal.ZERO);

        service.deletePurchaseOrder(1L);

        verify(purchaseOrderRepository, times(1)).delete(po);
    }

    @Test
    void deletePurchaseOrder_WithReceipts_ThrowsException() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(1L);
        po.setStatus(PurchaseOrderStatus.DRAFT); 

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(po));
        when(goodsReceiptRepository.calculateTotalReceivedQtyForPo(1L)).thenReturn(new BigDecimal("5.0"));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> service.deletePurchaseOrder(1L));
        assertTrue(exception.getMessage().contains("recorded goods receipts"));
        verify(purchaseOrderRepository, never()).delete(any());
    }

    @Test
    void deletePurchaseOrder_NotDraft_ThrowsException() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(1L);
        po.setStatus(PurchaseOrderStatus.SUBMITTED);

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(po));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> service.deletePurchaseOrder(1L));
        assertTrue(exception.getMessage().contains("Only DRAFT purchase orders can be deleted"));
        verify(purchaseOrderRepository, never()).delete(any());
    }

    @Test
    void cancelPurchaseOrder_DeactivatesSuccessfully() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(1L);
        po.setStatus(PurchaseOrderStatus.SUBMITTED);

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(po));

        service.cancelPurchaseOrder(1L);

        assertEquals(PurchaseOrderStatus.CANCELLED, po.getStatus());
        verify(purchaseOrderRepository, times(1)).save(po);
    }

    @Test
    void cancelPurchaseOrder_AlreadyReceived_ThrowsException() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(1L);
        po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(po));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> service.cancelPurchaseOrder(1L));
        assertTrue(exception.getMessage().contains("already been partially or fully received"));
        verify(purchaseOrderRepository, never()).save(po);
    }
}
