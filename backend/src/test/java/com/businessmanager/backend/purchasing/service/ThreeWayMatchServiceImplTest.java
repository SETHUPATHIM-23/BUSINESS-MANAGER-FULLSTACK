package com.businessmanager.backend.purchasing.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.purchasing.dto.SupplierInvoiceRequest;
import com.businessmanager.backend.purchasing.entity.PurchaseLine;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
import com.businessmanager.backend.purchasing.repository.GoodsReceiptRepository;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ThreeWayMatchServiceImplTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private GoodsReceiptRepository goodsReceiptRepository;
    @Mock private ProductRepository productRepository;
    @Mock private com.businessmanager.backend.accounting.repository.AccountMappingRepository accountMappingRepository;

    @InjectMocks
    private ThreeWayMatchServiceImpl service;

    private PurchaseOrder po;
    private Product product;
    private PurchaseLine line;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "toleranceAmount", new BigDecimal("10.00"));

        po = new PurchaseOrder();
        po.setId(1L);
        po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);

        product = new Product();
        product.setId(10L);
        product.setStockOnHand(new BigDecimal("100"));
        product.setCostPrice(new BigDecimal("50.00")); 

        line = new PurchaseLine();
        line.setId(100L);
        line.setProduct(product);
        line.setOrderedQty(new BigDecimal("10"));
        line.setCostPrice(new BigDecimal("50.00"));

        po.addLine(line);
    }

    @Test
    void processSupplierInvoice_ExactMatch_UpdatesStatusAndMAC() {
        SupplierInvoiceRequest request = new SupplierInvoiceRequest();
        request.setPurchaseOrderId(1L);
        request.setSupplierInvoicedSubtotal(new BigDecimal("500.00"));

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(po));
        when(goodsReceiptRepository.calculateTotalReceivedQtyForLine(100L)).thenReturn(new BigDecimal("10"));

        service.processSupplierInvoice(request);

        assertEquals(PurchaseOrderStatus.RECEIVED, po.getStatus());
        verify(purchaseOrderRepository, times(1)).save(po);
        verify(productRepository, never()).save(any()); 
    }

    @Test
    void processSupplierInvoice_MismatchExceedsTolerance_ThrowsException() {
        SupplierInvoiceRequest request = new SupplierInvoiceRequest();
        request.setPurchaseOrderId(1L);
        request.setSupplierInvoicedSubtotal(new BigDecimal("515.00")); 

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(po));
        when(goodsReceiptRepository.calculateTotalReceivedQtyForLine(100L)).thenReturn(new BigDecimal("10"));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> service.processSupplierInvoice(request));
        assertTrue(exception.getMessage().contains("Manual approval required"));
    }

    @Test
    void processSupplierInvoice_MismatchExceedsTolerance_WithForceApprove_Passes() {
        SupplierInvoiceRequest request = new SupplierInvoiceRequest();
        request.setPurchaseOrderId(1L);
        request.setSupplierInvoicedSubtotal(new BigDecimal("515.00"));
        request.setForceApprove(true);

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(po));
        when(goodsReceiptRepository.calculateTotalReceivedQtyForLine(100L)).thenReturn(new BigDecimal("10"));

        service.processSupplierInvoice(request);

        assertEquals(new BigDecimal("50.1500"), product.getCostPrice());
        verify(productRepository, times(1)).save(product);
        assertEquals(PurchaseOrderStatus.RECEIVED, po.getStatus());
    }

    @Test
    void processSupplierInvoice_LandedCosts_ApportionsCorrectly() {
        SupplierInvoiceRequest request = new SupplierInvoiceRequest();
        request.setPurchaseOrderId(1L);
        request.setSupplierInvoicedSubtotal(new BigDecimal("500.00"));
        request.setFreightAmount(new BigDecimal("20.00"));
        request.setDutiesAmount(new BigDecimal("30.00"));

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(po));
        when(goodsReceiptRepository.calculateTotalReceivedQtyForLine(100L)).thenReturn(new BigDecimal("10"));

        service.processSupplierInvoice(request);

        assertEquals(new BigDecimal("50.5000"), product.getCostPrice());
        verify(productRepository, times(1)).save(product);
    }
    
    @Test
    void processSupplierInvoice_NoGoodsReceived_ThrowsException() {
        SupplierInvoiceRequest request = new SupplierInvoiceRequest();
        request.setPurchaseOrderId(1L);
        request.setSupplierInvoicedSubtotal(new BigDecimal("500.00"));

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(po));
        when(goodsReceiptRepository.calculateTotalReceivedQtyForLine(100L)).thenReturn(BigDecimal.ZERO);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> service.processSupplierInvoice(request));
        assertTrue(exception.getMessage().contains("No goods have been received"));
    }
}
