package com.businessmanager.backend.billing.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.AccountMapping;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.repository.AccountMappingRepository;
import com.businessmanager.backend.accounting.repository.JournalEntryRepository;
import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.entity.InvoiceLine;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import com.businessmanager.backend.billing.repository.InvoiceLineRepository;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import com.businessmanager.backend.product.entity.InventoryTransaction;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.repository.InventoryTransactionRepository;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.fund.repository.FundAccountRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceLineRepository invoiceLineRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private AccountMappingRepository accountMappingRepository;
    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private InvoicePricingService invoicePricingService;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private FundAccountRepository fundAccountRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Customer customer;
    private Product product;
    private Invoice invoice;
    private InvoiceLine invoiceLine;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setName("Acme Corp");
        customer.setCreditHold(false);
        customer.setCreditLimit(new BigDecimal("10000.00"));

        product = new Product();
        product.setId(10L);
        product.setSku("PROD-01");
        product.setName("Widget");
        product.setBaseSellingPrice(new BigDecimal("100.00"));
        product.setCostPrice(new BigDecimal("40.00"));
        product.setStockOnHand(new BigDecimal("50.00"));
        product.setAllowNegativeStock(false);

        invoiceLine = new InvoiceLine();
        invoiceLine.setId(1L);
        invoiceLine.setProduct(product);
        invoiceLine.setQuantity(new BigDecimal("2.00"));
        invoiceLine.setUnitPrice(new BigDecimal("100.00"));
        invoiceLine.setDiscount(BigDecimal.ZERO);
        // lineTotal should be 200.00, cost is 80.00

        invoice = new Invoice();
        invoice.setId(100L);
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice.addLine(invoiceLine);
        // manual subtotal, tax, grand total for pre-setup objects where needed
        invoice.setSubtotal(new BigDecimal("200.00"));
        invoice.setTaxTotal(BigDecimal.ZERO);
        invoice.setGrandTotal(new BigDecimal("200.00"));
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Test
    void createInvoice_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix("INV-")).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        // Create new invoice object without lines resolved yet
        Invoice newInvoice = new Invoice();
        newInvoice.setCustomer(customer);
        InvoiceLine newLine = new InvoiceLine();
        Product pRef = new Product(); pRef.setId(10L);
        newLine.setProduct(pRef);
        newLine.setQuantity(new BigDecimal("5.00"));
        newInvoice.addLine(newLine);

        doAnswer(i -> {
            Invoice inv = i.getArgument(0);
            inv.setGrandTotal(new BigDecimal("500.00"));
            return null;
        }).when(invoicePricingService).calculateTotals(any(Invoice.class));

        Invoice result = invoiceService.createInvoice(newInvoice);

        assertNotNull(result);
        assertEquals(InvoiceStatus.DRAFT, result.getStatus());
        assertEquals("INV-000001", result.getInvoiceNumber());
        assertEquals(new BigDecimal("500.00"), result.getGrandTotal()); // 5 * 100
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createInvoice_CustomerCreditHold_ThrowsException() {
        customer.setCreditHold(true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertThrows(BusinessRuleException.class, () -> invoiceService.createInvoice(invoice));
    }

    @Test
    void createInvoice_DuplicateInvoiceNumber_ThrowsException() {
        invoice.setInvoiceNumber("INV-9999");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.existsByInvoiceNumber("INV-9999")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> invoiceService.createInvoice(invoice));
    }

    @Test
    void previewTotals_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        
        Invoice draft = new Invoice();
        draft.setCustomer(customer);
        InvoiceLine newLine = new InvoiceLine();
        Product pRef = new Product(); pRef.setId(10L);
        newLine.setProduct(pRef);
        newLine.setQuantity(new BigDecimal("2.00"));
        draft.addLine(newLine);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        
        Invoice preview = invoiceService.previewTotals(draft);

        assertNotNull(preview);
        assertEquals(InvoiceStatus.DRAFT, preview.getStatus());
        assertEquals(BigDecimal.ZERO, preview.getAmountPaid());
        
        verify(invoicePricingService).calculateTotals(preview);
        // It should NOT call save
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Test
    void updateInvoice_Success() {
        Invoice existingInvoice = new Invoice();
        existingInvoice.setId(100L);
        existingInvoice.setStatus(InvoiceStatus.DRAFT);
        existingInvoice.setInvoiceDate(LocalDate.now());
        existingInvoice.setCustomer(customer);
        existingInvoice.setDueDate(LocalDate.now().plusDays(30));

        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(existingInvoice));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice updateData = new Invoice();
        updateData.setCustomer(customer);
        InvoiceLine newLine = new InvoiceLine();
        newLine.setProduct(product);
        newLine.setQuantity(new BigDecimal("3.00"));
        updateData.addLine(newLine);

        doAnswer(i -> {
            Invoice inv = i.getArgument(0);
            inv.setGrandTotal(new BigDecimal("300.00"));
            return null;
        }).when(invoicePricingService).calculateTotals(any(Invoice.class));

        Invoice result = invoiceService.updateInvoice(100L, updateData);

        assertEquals(new BigDecimal("300.00"), result.getGrandTotal());
        assertEquals(1, result.getLines().size());
        verify(invoiceRepository).save(existingInvoice);
    }

    @Test
    void updateInvoice_NotDraft_ThrowsException() {
        invoice.setStatus(InvoiceStatus.SENT);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessRuleException.class, () -> invoiceService.updateInvoice(100L, invoice));
    }

    // ── Cancel and Delete ───────────────────────────────────────────────

    @Test
    void cancelInvoice_Draft_Success() {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        invoiceService.cancelInvoice(100L);

        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void cancelInvoice_WithPayment_VoidsInvoice() {
        invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        invoice.setAmountPaid(new BigDecimal("50.00"));
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        JournalEntry je = new JournalEntry();
        je.addLine(new com.businessmanager.backend.accounting.entity.JournalLine());
        when(journalEntryRepository.findByReferenceTypeAndReferenceId("INVOICE", 100L)).thenReturn(java.util.List.of(je));

        invoiceService.cancelInvoice(100L);

        assertEquals(InvoiceStatus.VOIDED, invoice.getStatus());
        verify(invoiceRepository).save(invoice);
        // Verify inventory restoration
        verify(productRepository, atLeastOnce()).save(any(Product.class));
        verify(inventoryTransactionRepository, atLeastOnce()).save(any(InventoryTransaction.class));
        // Verify reversal journal entry
        verify(journalEntryRepository, atLeastOnce()).save(any(JournalEntry.class));
    }

    @Test
    void deleteInvoice_Success() {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        invoiceService.deleteInvoice(100L);

        verify(invoiceRepository).delete(invoice);
    }

    @Test
    void deleteInvoice_NotDraft_ThrowsException() {
        invoice.setStatus(InvoiceStatus.SENT);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessRuleException.class, () -> invoiceService.deleteInvoice(100L));
    }

    // ── Payment Recording ───────────────────────────────────────────────

    @Test
    void recordPayment_PartialPayment_ChangesStatusToPartiallyPaid() {
        invoice.setStatus(InvoiceStatus.SENT);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Account acc = new Account(); acc.setId(2L);
        when(accountRepository.findById(2L)).thenReturn(Optional.of(acc));
        when(accountMappingRepository.findByMappingKey(anyString())).thenReturn(Optional.of(new AccountMapping()));
        when(fundAccountRepository.findByGlAccountId(2L)).thenReturn(Optional.empty());

        Invoice result = invoiceService.recordPayment(100L, 2L, new BigDecimal("50.00"));

        assertEquals(InvoiceStatus.PARTIALLY_PAID, result.getStatus());
        assertEquals(new BigDecimal("50.00"), result.getAmountPaid());
    }

    @Test
    void recordPayment_FullPayment_ChangesStatusToPaid() {
        invoice.setStatus(InvoiceStatus.SENT);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        // Mock account resolution for payment
        Account acc = new Account(); acc.setId(2L);
        when(accountRepository.findById(2L)).thenReturn(Optional.of(acc));
        when(accountMappingRepository.findByMappingKey(anyString())).thenReturn(Optional.of(new AccountMapping()));
        when(fundAccountRepository.findByGlAccountId(2L)).thenReturn(Optional.empty());
        
        Invoice result = invoiceService.recordPayment(100L, 2L, new BigDecimal("200.00"));

        assertEquals(InvoiceStatus.PAID, result.getStatus());
        assertEquals(new BigDecimal("200.00"), result.getAmountPaid());
    }

    @Test
    void recordPayment_ExceedsBalance_ThrowsException() {
        invoice.setStatus(InvoiceStatus.SENT);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessRuleException.class, () -> invoiceService.recordPayment(100L, 2L, new BigDecimal("250.00")));
    }

    @Test
    void postAndPayInvoice_Success() {
        // Arrange for Post
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix(anyString())).thenReturn(Optional.of("INV-000005"));
        when(invoiceRepository.existsByInvoiceNumber("INV-000006")).thenReturn(false);
        when(invoiceRepository.calculateOutstandingBalanceByCustomerId(1L)).thenReturn(new BigDecimal("5000.00"));
        
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        
        Account acc = new Account(); acc.setId(99L);
        AccountMapping mapping = new AccountMapping(); mapping.setAccount(acc);
        when(accountMappingRepository.findByMappingKey(anyString())).thenReturn(Optional.of(mapping));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Account paymentAcc = new Account(); paymentAcc.setId(2L);
        when(accountRepository.findById(2L)).thenReturn(Optional.of(paymentAcc));
        when(fundAccountRepository.findByGlAccountId(2L)).thenReturn(Optional.empty());

        // Act
        Invoice result = invoiceService.postAndPayInvoice(100L, 2L, new BigDecimal("200.00"));

        // Assert Status Transitions
        assertEquals(InvoiceStatus.PAID, result.getStatus());
        assertEquals(new BigDecimal("200.00"), result.getAmountPaid());
        assertEquals("INV-000006", result.getInvoiceNumber());
        
        // Assert inventory & journal calls (once for post, once for pay)
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
        verify(journalEntryRepository, times(2)).save(any(JournalEntry.class)); // 1 for Invoice, 1 for Payment
    }

    // ── Post Invoice Transaction (BILL-030/040/050) ─────────────────────

    @Test
    void postInvoice_Success() {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix(anyString())).thenReturn(Optional.of("INV-000005"));
        when(invoiceRepository.existsByInvoiceNumber("INV-000006")).thenReturn(false);
        when(invoiceRepository.calculateOutstandingBalanceByCustomerId(1L)).thenReturn(new BigDecimal("5000.00")); // Customer has 5k out of 10k limit
        
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        
        // Mock account resolution
        Account acc = new Account(); acc.setId(99L);
        AccountMapping mapping = new AccountMapping(); mapping.setAccount(acc);
        when(accountMappingRepository.findByMappingKey(anyString())).thenReturn(Optional.of(mapping));
        
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.postInvoice(100L);

        // 1. Status transition
        assertEquals(InvoiceStatus.SENT, result.getStatus());
        
        // 2. Invoice number assignment
        assertEquals("INV-000006", result.getInvoiceNumber());
        
        // 3. Inventory decrement
        assertEquals(new BigDecimal("48.00"), product.getStockOnHand()); // 50 - 2
        verify(productRepository).save(product);
        
        // 4. Inventory Transaction generated
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
        
        // 5. Journal Entry generated
        verify(journalEntryRepository).save(any(JournalEntry.class));
    }

    @Test
    void postInvoice_InvoiceNumberCollision_ThrowsException() {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix(anyString())).thenReturn(Optional.of("INV-000005"));
        when(invoiceRepository.existsByInvoiceNumber("INV-000006")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> invoiceService.postInvoice(100L));
    }

    @Test
    void postInvoice_CreditLimitExceeded_ThrowsException() {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix(anyString())).thenReturn(Optional.of("INV-000005"));
        when(invoiceRepository.existsByInvoiceNumber("INV-000006")).thenReturn(false);
        // Customer has 9900 out of 10k limit. Invoice is 200. 9900 + 200 = 10100 > 10000
        when(invoiceRepository.calculateOutstandingBalanceByCustomerId(1L)).thenReturn(new BigDecimal("9900.00"));

        assertThrows(BusinessRuleException.class, () -> invoiceService.postInvoice(100L));
    }

    @Test
    void postInvoice_InsufficientStock_Succeeds() {
        product.setStockOnHand(new BigDecimal("1.00")); // Need 2
        product.setAllowNegativeStock(false);

        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.calculateOutstandingBalanceByCustomerId(1L)).thenReturn(new BigDecimal("5000.00"));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        
        Account acc = new Account(); acc.setId(99L);
        AccountMapping mapping = new AccountMapping(); mapping.setAccount(acc);
        when(accountMappingRepository.findByMappingKey(anyString())).thenReturn(Optional.of(mapping));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.postInvoice(100L);

        assertEquals(InvoiceStatus.SENT, result.getStatus());
        assertEquals(new BigDecimal("-1.00"), product.getStockOnHand());
    }

    @Test
    void postInvoice_InsufficientStockButNegativeAllowed_Success() {
        product.setStockOnHand(new BigDecimal("1.00")); // Need 2
        product.setAllowNegativeStock(true);

        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix(anyString())).thenReturn(Optional.of("INV-000005"));
        when(invoiceRepository.existsByInvoiceNumber("INV-000006")).thenReturn(false);
        when(invoiceRepository.calculateOutstandingBalanceByCustomerId(1L)).thenReturn(new BigDecimal("5000.00"));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        
        Account acc = new Account(); acc.setId(99L);
        AccountMapping mapping = new AccountMapping(); mapping.setAccount(acc);
        when(accountMappingRepository.findByMappingKey(anyString())).thenReturn(Optional.of(mapping));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.postInvoice(100L);

        assertEquals(InvoiceStatus.SENT, result.getStatus());
        assertEquals(new BigDecimal("-1.00"), product.getStockOnHand()); // 1 - 2
    }

    // ── Sales Returns ───────────────────────────────────────────────────

    @Test
    void processReturn_Success() {
        invoice.setStatus(InvoiceStatus.SENT);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findLatestInvoiceNumberByPrefix("CN-")).thenReturn(Optional.of("CN-000001"));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Account acc = new Account(); acc.setId(99L);
        AccountMapping mapping = new AccountMapping(); mapping.setAccount(acc);
        when(accountMappingRepository.findByMappingKey(anyString())).thenReturn(Optional.of(mapping));

        java.util.Map<Long, BigDecimal> returnMap = new java.util.HashMap<>();
        returnMap.put(invoiceLine.getId(), new BigDecimal("1.00")); // returning 1 qty

        Invoice creditNote = invoiceService.processReturn(100L, returnMap);

        assertNotNull(creditNote);
        assertEquals("CN-000002", creditNote.getInvoiceNumber());
        assertEquals("CREDIT_NOTE", creditNote.getBillType());
        assertEquals(invoice.getGstPercentage(), creditNote.getGstPercentage());
        assertEquals(1, creditNote.getLines().size());
        assertEquals(new BigDecimal("-1.00"), creditNote.getLines().get(0).getQuantity());

        verify(invoicePricingService).calculateTotals(any(Invoice.class));
        verify(productRepository, atLeastOnce()).save(any(Product.class));
        verify(inventoryTransactionRepository, atLeastOnce()).save(any(InventoryTransaction.class));
        verify(journalEntryRepository, atLeastOnce()).save(any(JournalEntry.class));
    }
}
