package com.businessmanager.backend.purchasing.controller;

import com.businessmanager.backend.product.entity.InventoryTransaction;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.repository.InventoryTransactionRepository;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.purchasing.dto.GoodsReceiptRequest;
import com.businessmanager.backend.purchasing.dto.PurchaseLineRequest;
import com.businessmanager.backend.purchasing.dto.PurchaseOrderCreateRequest;
import com.businessmanager.backend.purchasing.dto.SupplierInvoiceRequest;
import com.businessmanager.backend.purchasing.entity.PurchaseLine;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
import com.businessmanager.backend.purchasing.repository.GoodsReceiptRepository;
import com.businessmanager.backend.purchasing.repository.PurchaseLineRepository;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import com.businessmanager.backend.supplier.entity.Supplier;
import com.businessmanager.backend.supplier.repository.SupplierRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PurchaseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private PurchaseOrderRepository purchaseOrderRepository;
    @MockBean private PurchaseLineRepository purchaseLineRepository;
    @MockBean private GoodsReceiptRepository goodsReceiptRepository;
    @MockBean private SupplierRepository supplierRepository;
    @MockBean private ProductRepository productRepository;
    @MockBean private InventoryTransactionRepository inventoryTransactionRepository;

    @MockBean private JwtTokenProvider tokenProvider;
    @MockBean private CustomUserDetailsService userDetailsService;

    private PurchaseOrderCreateRequest createRequest;
    private Supplier supplier;
    private Product product;
    private PurchaseOrder draftPo;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Tech Corp");

        product = new Product();
        product.setId(10L);
        product.setSku("WIDGET");
        product.setCostPrice(new BigDecimal("50.00"));
        product.setStockOnHand(new BigDecimal("100.00"));

        PurchaseLineRequest lineRequest = new PurchaseLineRequest();
        lineRequest.setProductId(10L);
        lineRequest.setOrderedQty(new BigDecimal("10.00"));
        lineRequest.setCostPrice(new BigDecimal("50.00"));

        createRequest = new PurchaseOrderCreateRequest();
        createRequest.setSupplierId(1L);
        createRequest.setLines(Collections.singletonList(lineRequest));

        draftPo = new PurchaseOrder();
        draftPo.setId(100L);
        draftPo.setSupplier(supplier);
        draftPo.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);

        PurchaseLine line = new PurchaseLine();
        line.setId(1000L);
        line.setProduct(product);
        line.setOrderedQty(new BigDecimal("10.00"));
        line.setCostPrice(new BigDecimal("50.00"));
        line.setPurchaseOrder(draftPo);

        draftPo.addLine(line);
    }

    // ── Validation Tests ───────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "PURCHASE_WRITE")
    public void createPurchaseOrder_ValidationError_MissingSupplier_BadRequest() throws Exception {
        createRequest.setSupplierId(null); 

        mockMvc.perform(post("/api/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // ── Authorization Tests ────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "PURCHASE_WRITE")
    public void createPurchaseOrder_Authorized_Success() throws Exception {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(purchaseOrderRepository.findLatestPoNumberByPrefix("PO-")).thenReturn(Optional.of("PO-000000"));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(i -> {
            PurchaseOrder saved = i.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        mockMvc.perform(post("/api/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.poNumber").value("PO-000001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(500.0));
    }

    @Test
    @WithMockUser(authorities = "BILLING_WRITE")
    public void createPurchaseOrder_UnauthorizedRole_Forbidden() throws Exception {
        mockMvc.perform(post("/api/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    // ── Full End-to-End Business Flow via API ──────────────────────────

    @Test
    @WithMockUser(authorities = "PURCHASE_WRITE")
    public void receiveGoods_Authorized_Success() throws Exception {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(100L);
        po.setStatus(PurchaseOrderStatus.SUBMITTED);
        
        PurchaseLine poLine = new PurchaseLine();
        poLine.setId(1000L);
        poLine.setProduct(product);
        poLine.setPurchaseOrder(po);
        
        GoodsReceiptRequest.GoodsReceiptLineRequest grLine = new GoodsReceiptRequest.GoodsReceiptLineRequest();
        grLine.setPurchaseLineId(1000L);
        grLine.setReceivedQty(new BigDecimal("5.00"));

        GoodsReceiptRequest grReq = new GoodsReceiptRequest();
        grReq.setPoId(100L);
        grReq.setReceivedDate(LocalDate.now());
        grReq.setLines(Collections.singletonList(grLine));

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(po));
        when(purchaseLineRepository.findById(1000L)).thenReturn(Optional.of(poLine));

        mockMvc.perform(post("/api/purchases/100/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grReq)))
                .andExpect(status().isOk());

        verify(goodsReceiptRepository, times(1)).save(any());
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
    }

    @Test
    @WithMockUser(authorities = "PURCHASE_WRITE")
    public void processSupplierInvoice_ThreeWayMatch_ToleranceExceeded_Conflict() throws Exception {
        SupplierInvoiceRequest invoiceReq = new SupplierInvoiceRequest();
        invoiceReq.setPurchaseOrderId(100L);
        invoiceReq.setSupplierInvoicedSubtotal(new BigDecimal("550.00")); 

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(draftPo));
        when(goodsReceiptRepository.calculateTotalReceivedQtyForLine(1000L)).thenReturn(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/purchases/100/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Manual approval required")));
    }

    @Test
    @WithMockUser(authorities = "PURCHASE_WRITE")
    public void processSupplierInvoice_ThreeWayMatch_SuccessWithLandedCosts() throws Exception {
        SupplierInvoiceRequest invoiceReq = new SupplierInvoiceRequest();
        invoiceReq.setPurchaseOrderId(100L);
        invoiceReq.setSupplierInvoicedSubtotal(new BigDecimal("500.00"));
        invoiceReq.setFreightAmount(new BigDecimal("20.00"));

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(draftPo));
        when(goodsReceiptRepository.calculateTotalReceivedQtyForLine(1000L)).thenReturn(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/purchases/100/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceReq)))
                .andExpect(status().isOk());

        verify(productRepository, times(1)).save(any(Product.class));
        verify(purchaseOrderRepository, times(1)).save(draftPo);
        assert(draftPo.getStatus() == PurchaseOrderStatus.RECEIVED);
    }
}
