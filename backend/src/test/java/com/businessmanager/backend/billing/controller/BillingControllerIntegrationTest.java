package com.businessmanager.backend.billing.controller;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.AccountMapping;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.repository.AccountMappingRepository;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.repository.JournalEntryRepository;
import com.businessmanager.backend.fund.repository.FundAccountRepository;
import com.businessmanager.backend.billing.dto.InvoiceCreateRequest;
import com.businessmanager.backend.billing.dto.InvoiceLineRequest;
import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.entity.InvoiceLine;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import com.businessmanager.backend.billing.repository.InvoiceLineRepository;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import com.businessmanager.backend.product.entity.InventoryTransaction;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.repository.InventoryTransactionRepository;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import com.businessmanager.backend.common.event.CustomerPaymentPostedEvent;
import com.businessmanager.backend.fund.entity.FundAccount;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BillingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock all repositories to isolate the test from the database
    // while executing the real Controller + Service layers
    @MockBean
    private InvoiceRepository invoiceRepository;
    @MockBean
    private InvoiceLineRepository invoiceLineRepository;
    @MockBean
    private CustomerRepository customerRepository;
    @MockBean
    private ProductRepository productRepository;
    @MockBean
    private InventoryTransactionRepository inventoryTransactionRepository;
    @MockBean
    private AccountMappingRepository accountMappingRepository;
    @MockBean
    private JournalEntryRepository journalEntryRepository;
    @MockBean
    private AccountRepository accountRepository;
    @MockBean
    private FundAccountRepository fundAccountRepository;
    @MockBean
    private com.businessmanager.backend.fund.service.FundAccountService fundAccountService;
    @MockBean
    private com.businessmanager.backend.common.audit.repository.AuditLogRepository auditLogRepository;
    @MockBean
    private com.businessmanager.backend.fund.service.FundTransactionService fundTransactionService;

    // Mock security components
    @MockBean
    private JwtTokenProvider tokenProvider;
    @MockBean
    private CustomUserDetailsService userDetailsService;

    private InvoiceCreateRequest createRequest;
    private Customer customer;
    private Product product;
    private Invoice draftInvoice;

    @BeforeEach
    void setUp() {
        // Setup Customer
        customer = new Customer();
        customer.setId(1L);
        customer.setName("Acme Corp");
        customer.setCreditHold(false);
        customer.setCreditLimit(new BigDecimal("10000.00"));

        // Setup Product
        product = new Product();
        product.setId(10L);
        product.setSku("PROD-01");
        product.setName("Widget");
        product.setBaseSellingPrice(new BigDecimal("100.00"));
        product.setCostPrice(new BigDecimal("40.00"));
        product.setStockOnHand(new BigDecimal("50.00"));
        product.setAllowNegativeStock(false);

        // Setup Request DTO
        InvoiceLineRequest lineRequest = new InvoiceLineRequest();
        lineRequest.setProductId(10L);
        lineRequest.setQuantity(new BigDecimal("2.00"));

        createRequest = new InvoiceCreateRequest();
        createRequest.setCustomerId(1L);
        createRequest.setBillType("tax_exclusive");
        createRequest.setLines(Collections.singletonList(lineRequest));

        // Setup Existing DRAFT Invoice for posting
        draftInvoice = new Invoice();
        draftInvoice.setId(100L);
        draftInvoice.setCustomer(customer);
        draftInvoice.setStatus(InvoiceStatus.DRAFT);
        draftInvoice.setAmountPaid(BigDecimal.ZERO);
        draftInvoice.setSubtotal(new BigDecimal("200.00"));
        draftInvoice.setTaxTotal(BigDecimal.ZERO);
        draftInvoice.setGrandTotal(new BigDecimal("200.00"));
        
        InvoiceLine line = new InvoiceLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("2.00"));
        line.setUnitPrice(new BigDecimal("100.00"));
        line.setDiscount(BigDecimal.ZERO);
        line.setTaxAmount(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("200.00"));
        draftInvoice.addLine(line);
    }

    // ── Validation Tests ───────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "BILLING_WRITE")
    public void createInvoice_ValidationError_MissingCustomer_BadRequest() throws Exception {
        createRequest.setCustomerId(null); // Invalid: customerId is required

        mockMvc.perform(post("/api/billings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // ── Authorization Tests ────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "BILLING_WRITE")
    public void createInvoice_Authorized_Success() throws Exception {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix("INV-")).thenReturn(Optional.of("INV-000000"));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/billings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-000001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.grandTotal").value(236.0));
    }

    @Test
    @WithMockUser(authorities = "BILLING_READ")
    public void createInvoice_UnauthorizedRole_Forbidden() throws Exception {
        // Only BILLING_WRITE can create
        InvoiceCreateRequest req = new InvoiceCreateRequest();
        req.setBillType("tax_exclusive");
        req.setCustomerId(1L);
        InvoiceLineRequest line = new InvoiceLineRequest();
        line.setProductId(10L);
        line.setQuantity(new BigDecimal("2.00"));
        req.setLines(java.util.Collections.singletonList(line));

        mockMvc.perform(post("/api/billings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BILLING_READ")
    public void getInvoiceById_Authorized_Success() throws Exception {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(draftInvoice));

        mockMvc.perform(get("/api/billings/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    // ── Full End-to-End Business Flow via API (BILL-030, BILL-040, BILL-050)

    @Test
    @WithMockUser(authorities = "BILLING_POST")
    public void postInvoice_Authorized_FullBusinessFlow_Success() throws Exception {
        // Setup Mocks for the full transaction
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(draftInvoice));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix("INV-")).thenReturn(Optional.of("INV-000099"));
        when(invoiceRepository.existsByInvoiceNumber("INV-000100")).thenReturn(false);
        when(invoiceRepository.calculateOutstandingBalanceByCustomerId(1L)).thenReturn(new BigDecimal("5000.00"));
        
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        
        // Mock account resolution for journal entries
        Account acc = new Account(); acc.setId(99L);
        AccountMapping mapping = new AccountMapping(); mapping.setAccount(acc);
        when(accountMappingRepository.findByMappingKey(anyString())).thenReturn(Optional.of(mapping));
        
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        // Execute API Call
        mockMvc.perform(post("/api/billings/100/post"))
                .andExpect(status().isOk())
                // Assert Status Transition
                .andExpect(jsonPath("$.status").value("SENT"))
                // Assert Gap-Free Numbering (BILL-030)
                .andExpect(jsonPath("$.invoiceNumber").value("INV-000100"));

        // Verify Inventory Decrement (BILL-040, BILL-050)
        verify(productRepository, times(1)).save(product);
        // Initial was 50, sold 2, should be 48
        assert(product.getStockOnHand().compareTo(new BigDecimal("48.00")) == 0);
        
        // Verify InventoryTransaction Audit generated
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
        
        // Verify Balanced Journal Entry generated
        verify(journalEntryRepository, times(1)).save(any(JournalEntry.class));
    }

    @Test
    @WithMockUser(authorities = "BILLING_POST")
    public void postAndPayInvoice_Authorized_FullBusinessFlow_Success() throws Exception {
        // Setup Mocks for the full transaction
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(draftInvoice));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix("INV-")).thenReturn(Optional.of("INV-000099"));
        when(invoiceRepository.existsByInvoiceNumber("INV-000100")).thenReturn(false);
        when(invoiceRepository.calculateOutstandingBalanceByCustomerId(1L)).thenReturn(new BigDecimal("5000.00"));
        
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        
        // Mock account resolution for journal entries
        Account acc = new Account(); acc.setId(99L);
        AccountMapping mapping = new AccountMapping(); mapping.setAccount(acc);
        when(accountMappingRepository.findByMappingKey(anyString())).thenReturn(Optional.of(mapping));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Account paymentAcc = new Account(); paymentAcc.setId(2L);
        when(accountRepository.findById(2L)).thenReturn(Optional.of(paymentAcc));
        FundAccount fundAcc = new FundAccount(); fundAcc.setId(5L);
        when(fundAccountRepository.findByGlAccountId(2L)).thenReturn(Optional.of(fundAcc));

        // Mock payment details
        com.businessmanager.backend.billing.dto.RecordPaymentRequest payRequest = new com.businessmanager.backend.billing.dto.RecordPaymentRequest();
        payRequest.setPaymentAccountId(2L);
        payRequest.setAmount(new BigDecimal("200.00"));

        // Execute API Call
        mockMvc.perform(post("/api/billings/100/post-and-pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payRequest)))
                .andExpect(status().isOk())
                // Assert Status Transition
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.amountPaid").value(200.0))
                // Assert Gap-Free Numbering (BILL-030)
                .andExpect(jsonPath("$.invoiceNumber").value("INV-000100"));

        // Verify Inventory Decrement (BILL-040, BILL-050)
        verify(productRepository, times(1)).save(product);
        assert(product.getStockOnHand().compareTo(new BigDecimal("48.00")) == 0);
        
        // Verify InventoryTransaction Audit generated
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
        
        // Verify Balanced Journal Entry generated (1 for invoice, 1 for payment = 2)
        verify(journalEntryRepository, times(2)).save(any(JournalEntry.class));
    }

    @Test
    @WithMockUser(authorities = "BILLING_WRITE")
    public void postInvoice_UnauthorizedRole_Forbidden() throws Exception {
        // Only BILLING_POST can post invoices
        mockMvc.perform(post("/api/billings/100/post"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BILLING_POST")
    public void postInvoice_CreditLimitExceeded_Conflict() throws Exception {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(draftInvoice));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix("INV-")).thenReturn(Optional.of("INV-000099"));
        when(invoiceRepository.existsByInvoiceNumber("INV-000100")).thenReturn(false);
        // Set outstanding to 9900. Invoice is 200. Total 10100 > 10000 limit
        when(invoiceRepository.calculateOutstandingBalanceByCustomerId(1L)).thenReturn(new BigDecimal("9900.00"));

        mockMvc.perform(post("/api/billings/100/post"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("exceed the customer's credit limit")));
    }

}
