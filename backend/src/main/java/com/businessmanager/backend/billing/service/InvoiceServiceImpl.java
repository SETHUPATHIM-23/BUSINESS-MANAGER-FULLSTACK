package com.businessmanager.backend.billing.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.entity.InvoiceLine;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import com.businessmanager.backend.billing.repository.InvoiceLineRepository;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.entity.InventoryTransaction;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.product.repository.InventoryTransactionRepository;
import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.enums.AccountType;
import com.businessmanager.backend.accounting.entity.AccountMapping;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.entity.JournalLine;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.repository.AccountMappingRepository;
import com.businessmanager.backend.accounting.repository.JournalEntryRepository;
import com.businessmanager.backend.common.event.CustomerPaymentPostedEvent;
import com.businessmanager.backend.fund.repository.FundAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final AccountMappingRepository accountMappingRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final FundAccountRepository fundAccountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InvoicePricingService invoicePricingService;

    // ── Create ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction(action = "CREATE", module = "BILLING")
    public Invoice createInvoice(Invoice invoice) {
        // Resolve and validate customer
        if (invoice.getCustomer() == null || invoice.getCustomer().getId() == null) {
            throw new BusinessRuleException("Customer is required when creating an invoice.");
        }
        Customer customer = customerRepository.findById(invoice.getCustomer().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + invoice.getCustomer().getId()));

        // Credit-hold check
        if (customer.isCreditHold()) {
            throw new BusinessRuleException(
                    "Cannot create invoice: customer '" + customer.getName()
                    + "' is on credit hold.");
        }

        invoice.setCustomer(customer);

        // Generate invoice number if not provided, or auto-increment if duplicate
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            invoice.setInvoiceNumber(generateNextInvoiceNumber(""));
        } else {
            String candidate = invoice.getInvoiceNumber().trim();
            int safetyCounter = 0;
            while (invoiceRepository.existsByInvoiceNumber(candidate) && safetyCounter < 1000) {
                candidate = incrementInvoiceNumberString(candidate);
                safetyCounter++;
            }
            invoice.setInvoiceNumber(candidate);
        }

        // Default dates and time
        if (invoice.getInvoiceDate() == null) {
            invoice.setInvoiceDate(LocalDate.now());
        }
        if (invoice.getInvoiceTime() == null) {
            invoice.setInvoiceTime(java.time.LocalTime.now());
        }
        if (invoice.getDueDate() == null) {
            invoice.setDueDate(invoice.getInvoiceDate().plusDays(30));
        }
        if (invoice.getDueDate().isBefore(invoice.getInvoiceDate())) {
            throw new BusinessRuleException("Due date cannot be before invoice date.");
        }

        // Force initial status
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setAmountPaid(BigDecimal.ZERO);

        // Re-add lines via addLine() to ensure the bidirectional invoice back-reference
        // is set on each line (so invoice_id / foreign key is not null on INSERT).
        if (invoice.getLines() != null && !invoice.getLines().isEmpty()) {
            List<InvoiceLine> rawLines = new ArrayList<>(invoice.getLines());
            invoice.getLines().clear();
            for (InvoiceLine line : rawLines) {
                invoice.addLine(line);
            }
        }

        // Resolve product references and set defaults
        resolveAndComputeLines(invoice);

        // Compute pricing using the centralized service
        invoicePricingService.calculateTotals(invoice);

        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice previewTotals(Invoice invoice) {
        if (invoice.getCustomer() != null && invoice.getCustomer().getId() != null) {
            customerRepository.findById(invoice.getCustomer().getId()).ifPresent(invoice::setCustomer);
        }

        resolveAndComputeLines(invoice);
        invoicePricingService.calculateTotals(invoice);

        if (invoice.getStatus() == null) invoice.setStatus(InvoiceStatus.DRAFT);
        if (invoice.getAmountPaid() == null) invoice.setAmountPaid(BigDecimal.ZERO);
        
        return invoice;
    }

    // ── Read ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with number: " + invoiceNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Invoice> searchInvoices(String search, Long customerId, InvoiceStatus status,
                                        LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        return invoiceRepository.searchInvoices(search, customerId, status, dateFrom, dateTo, pageable);
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction(action = "UPDATE", module = "BILLING")
    public Invoice updateInvoice(Long id, Invoice updated) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));

        // Only DRAFT invoices can be edited
        if (existing.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT invoices can be edited. Current status: " + existing.getStatus());
        }

        // Update header fields
        if (updated.getCustomer() != null && updated.getCustomer().getId() != null) {
            Customer customer = customerRepository.findById(updated.getCustomer().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer not found with ID: " + updated.getCustomer().getId()));
            if (customer.isCreditHold()) {
                throw new BusinessRuleException(
                        "Cannot assign invoice to customer '" + customer.getName()
                        + "': customer is on credit hold.");
            }
            existing.setCustomer(customer);
        }

        if (updated.getInvoiceDate() != null) {
            existing.setInvoiceDate(updated.getInvoiceDate());
        }
        if (updated.getInvoiceTime() != null) {
            existing.setInvoiceTime(updated.getInvoiceTime());
        }
        if (updated.getDueDate() != null) {
            existing.setDueDate(updated.getDueDate());
        }
        if (existing.getDueDate().isBefore(existing.getInvoiceDate())) {
            throw new BusinessRuleException("Due date cannot be before invoice date.");
        }
        if (updated.getNotes() != null) {
            existing.setNotes(updated.getNotes());
        }
        if (updated.getBillType() != null) {
            existing.setBillType(updated.getBillType());
        }
        if (updated.getVehicleNo() != null) {
            existing.setVehicleNo(updated.getVehicleNo());
        }
        if (updated.getGstPercentage() != null) {
            existing.setGstPercentage(updated.getGstPercentage());
        }

        // Replace lines
        existing.getLines().clear();
        if (updated.getLines() != null) {
            for (InvoiceLine line : updated.getLines()) {
                existing.addLine(line);
            }
        }

        // Resolve products and set defaults
        resolveAndComputeLines(existing);

        // Compute pricing using the centralized service
        invoicePricingService.calculateTotals(existing);

        return invoiceRepository.save(existing);
    }

    // ── Status transitions ──────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction(action = "STATUS_CHANGE", module = "BILLING")
    public Invoice changeStatus(Long id, InvoiceStatus newStatus) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));

        validateStatusTransition(invoice.getStatus(), newStatus);
        invoice.setStatus(newStatus);

        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    @AuditAction(action = "CANCEL", module = "BILLING")
    public void cancelInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED
                || invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new BusinessRuleException("Invoice is already cancelled/voided.");
        }

        boolean needsReversal = (invoice.getStatus() != InvoiceStatus.DRAFT);

        if (invoice.getAmountPaid().compareTo(BigDecimal.ZERO) > 0 || needsReversal) {
            invoice.setStatus(InvoiceStatus.VOIDED);
        } else {
            invoice.setStatus(InvoiceStatus.CANCELLED);
        }

        if (needsReversal) {
            reversePostedInvoice(invoice);
        }

        invoiceRepository.save(invoice);
    }

    private void reversePostedInvoice(Invoice invoice) {
        // Stock maintenance disabled - no stock adjustments or inventory transactions saved.

        // 2. Reverse Journal Entry
        java.util.List<JournalEntry> originalEntries = journalEntryRepository.findByReferenceTypeAndReferenceId("INVOICE", invoice.getId());
        for (JournalEntry originalJe : originalEntries) {
            JournalEntry reversalJe = new JournalEntry();
            reversalJe.setEntryDate(java.time.LocalDate.now());
            reversalJe.setReferenceType("INVOICE_VOID");
            reversalJe.setReferenceId(invoice.getId());
            reversalJe.setReferenceNumber(invoice.getInvoiceNumber());
            reversalJe.setDescription("Reversal for voided invoice: " + invoice.getInvoiceNumber());

            for (JournalLine oldLine : originalJe.getLines()) {
                JournalLine newLine = new JournalLine();
                newLine.setAccount(oldLine.getAccount());
                newLine.setDebitAmount(oldLine.getCreditAmount()); // Swap
                newLine.setCreditAmount(oldLine.getDebitAmount()); // Swap
                newLine.setDescription("Reversal: " + oldLine.getDescription());
                reversalJe.addLine(newLine);
            }
            journalEntryRepository.save(reversalJe);
        }
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "BILLING")
    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));

        // Invoices that already have recorded payments cannot be deleted at all
        if (invoice.getAmountPaid() != null && invoice.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException(
                    "Cannot delete an invoice that has recorded payments. "
                    + "Please cancel or void the invoice instead.");
        }

        // Already cancelled/voided — just hard-delete the record
        if (invoice.getStatus() == InvoiceStatus.CANCELLED
                || invoice.getStatus() == InvoiceStatus.VOIDED) {
            invoiceRepository.delete(invoice);
            return;
        }

        // DRAFT invoices: hard-delete directly
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            invoiceRepository.delete(invoice);
            return;
        }

        // Non-DRAFT invoices with no payments: cancel/void first (reverse journals), then delete
        boolean needsReversal = true; // any posted status has journal entries
        if (needsReversal) {
            reversePostedInvoice(invoice);
        }
        // Hard-delete the invoice record after reversals are saved
        invoiceRepository.delete(invoice);
    }

    // ── Payment recording & POS Checkout ────────────────────────────────

    @Override
    @Transactional
    @AuditAction(action = "POST_AND_PAY", module = "BILLING")
    public Invoice postAndPayInvoice(Long id, Long paymentAccountId, BigDecimal amount) {
        Invoice posted = postInvoice(id);
        BigDecimal payAmount = amount;
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            payAmount = posted.getGrandTotal().subtract(posted.getAmountPaid());
        }
        if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
            return recordPayment(posted.getId(), paymentAccountId, payAmount);
        }
        return posted;
    }

    @Override
    @Transactional
    @AuditAction(action = "PAYMENT", module = "BILLING")
    public Invoice recordPayment(Long id, Long paymentAccountId, BigDecimal amount) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));

        BigDecimal payAmount = amount;
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            payAmount = invoice.getGrandTotal().subtract(invoice.getAmountPaid());
        }
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return invoice;
        }

        // Cannot pay cancelled / voided invoices
        if (invoice.getStatus() == InvoiceStatus.CANCELLED
                || invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new BusinessRuleException(
                    "Cannot record payment against a " + invoice.getStatus() + " invoice.");
        }

        BigDecimal newPaid = invoice.getAmountPaid().add(payAmount);
        BigDecimal outstanding = invoice.getGrandTotal().subtract(newPaid);

        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            newPaid = invoice.getGrandTotal();
            outstanding = BigDecimal.ZERO;
        }

        invoice.setAmountPaid(newPaid);

        // Auto-transition status
        if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        Account paymentAccount = null;
        if (paymentAccountId != null) {
            paymentAccount = accountRepository.findById(paymentAccountId).orElse(null);
        }
        if (paymentAccount == null) {
            paymentAccount = accountRepository.findByCode("1110")
                    .or(() -> accountRepository.findAll().stream()
                            .filter(a -> a.getType() == AccountType.ASSET)
                            .findFirst())
                    .orElseGet(() -> accountRepository.findAll().stream().findFirst().orElse(null));
        }

        if (paymentAccount != null) {
            createPaymentJournalEntry(invoice, paymentAccount, payAmount);
            final Long accountId = paymentAccount.getId();
            final BigDecimal finalPayAmount = payAmount;
            fundAccountRepository.findByGlAccountId(accountId).ifPresent(fundAccount -> {
                eventPublisher.publishEvent(new CustomerPaymentPostedEvent(
                        fundAccount.getId(),
                        invoice.getId(),
                        finalPayAmount,
                        LocalDate.now()
                ));
            });
        }

        return invoiceRepository.save(invoice);
    }

    /**
     * Creates a balanced double-entry journal entry for a received payment:
     *
     * DR  Cash / Bank Account       = amount
     *   CR  Accounts Receivable      = amount
     */
    private void createPaymentJournalEntry(Invoice invoice, Account cashAccount, BigDecimal amount) {
        // Resolve mapped accounts
        Account arAccount = resolveAccount("ACCOUNTS_RECEIVABLE");

        JournalEntry je = new JournalEntry();
        je.setEntryDate(LocalDate.now());
        je.setReferenceType("PAYMENT_RECEIPT");
        je.setReferenceId(invoice.getId());
        je.setReferenceNumber("PAY-" + invoice.getInvoiceNumber());
        je.setDescription("Payment received for Invoice: " + invoice.getInvoiceNumber()
                + " — Customer: " + invoice.getCustomer().getName());

        // DR Cash
        JournalLine cashDebit = new JournalLine();
        cashDebit.setAccount(cashAccount);
        cashDebit.setDebitAmount(amount);
        cashDebit.setCreditAmount(BigDecimal.ZERO);
        cashDebit.setDescription("Payment Receipt — " + invoice.getInvoiceNumber());
        je.addLine(cashDebit);

        // CR Accounts Receivable
        JournalLine arCredit = new JournalLine();
        arCredit.setAccount(arAccount);
        arCredit.setDebitAmount(BigDecimal.ZERO);
        arCredit.setCreditAmount(amount);
        arCredit.setDescription("AR Reduction — " + invoice.getInvoiceNumber());
        je.addLine(arCredit);

        journalEntryRepository.save(je);
    }

    // ── Aggregate queries ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCustomerOutstandingBalance(Long customerId) {
        return invoiceRepository.calculateOutstandingBalanceByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        return invoiceRepository.calculateTotalRevenue();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingBalance() {
        return invoiceRepository.calculateTotalOutstandingBalance();
    }

    // ── Invoice number generation ───────────────────────────────────────

    private String incrementInvoiceNumberString(String str) {
        if (str == null || str.isBlank()) return "1001";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(.*?)(\\d+)$").matcher(str.trim());
        if (!matcher.matches()) {
            return str.trim() + "-0001";
        }
        String prefix = matcher.group(1);
        String digitsStr = matcher.group(2);
        try {
            java.math.BigInteger nextVal = new java.math.BigInteger(digitsStr).add(java.math.BigInteger.ONE);
            String nextStr = nextVal.toString();
            String padded = nextStr.length() < digitsStr.length() ? 
                    String.format("%" + digitsStr.length() + "s", nextStr).replace(' ', '0') : nextStr;
            return prefix + padded;
        } catch (Exception e) {
            return str.trim() + "-1";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String generateNextInvoiceNumber(String prefix) {
        if (prefix == null) prefix = "";
        Optional<String> latest = invoiceRepository.findLatestInvoiceNumberByPrefix(prefix);
        if (latest.isPresent()) {
            String val = latest.get();
            return incrementInvoiceNumberString(val);
        }
        return prefix + "1001";
    }

    // ── Sales Returns ───────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction(action = "RETURN", module = "BILLING")
    public Invoice processReturn(Long originalInvoiceId, java.util.Map<Long, BigDecimal> returnedQuantities) {
        Invoice original = invoiceRepository.findById(originalInvoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + originalInvoiceId));

        if (original.getStatus() == InvoiceStatus.DRAFT || original.getStatus() == InvoiceStatus.CANCELLED || original.getStatus() == InvoiceStatus.VOIDED) {
            throw new BusinessRuleException("Cannot process return for an invoice in status: " + original.getStatus());
        }

        Invoice creditNote = new Invoice();
        creditNote.setInvoiceNumber(generateNextInvoiceNumber("CN-"));
        creditNote.setCustomer(original.getCustomer());
        creditNote.setInvoiceDate(java.time.LocalDate.now());
        creditNote.setDueDate(java.time.LocalDate.now());
        creditNote.setStatus(InvoiceStatus.POSTED);
        creditNote.setBillType("CREDIT_NOTE");
        creditNote.setGstPercentage(original.getGstPercentage());
        creditNote.setNotes("Credit Note for return against Invoice " + original.getInvoiceNumber());

        for (java.util.Map.Entry<Long, BigDecimal> entry : returnedQuantities.entrySet()) {
            Long lineId = entry.getKey();
            BigDecimal returnQty = entry.getValue();

            if (returnQty == null || returnQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            InvoiceLine originalLine = original.getLines().stream()
                    .filter(l -> l.getId().equals(lineId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("Line item not found: " + lineId));

            if (returnQty.compareTo(originalLine.getQuantity()) > 0) {
                throw new BusinessRuleException("Return quantity " + returnQty + " exceeds original quantity " + originalLine.getQuantity());
            }

            InvoiceLine returnLine = new InvoiceLine();
            returnLine.setProduct(originalLine.getProduct());
            returnLine.setQuantity(returnQty.negate());
            returnLine.setUnitPrice(originalLine.getUnitPrice());
            creditNote.addLine(returnLine);
        }

        if (creditNote.getLines().isEmpty()) {
            throw new BusinessRuleException("No valid return quantities provided.");
        }

        invoicePricingService.calculateTotals(creditNote);

        Invoice savedCreditNote = invoiceRepository.save(creditNote);

        // Stock maintenance disabled - no stock adjustments or inventory transactions saved for credit notes.

        createInvoiceJournalEntry(savedCreditNote);

        return savedCreditNote;
    }

    // ── Summary Metrics ─────────────────────────────────────────────────

    /**
     * Resolve product references on each line and compute per-line totals.
     */
    private void resolveAndComputeLines(Invoice invoice) {
        if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
            return;
        }

        for (InvoiceLine line : invoice.getLines()) {
            // Resolve product
            if (line.getProduct() == null || line.getProduct().getId() == null) {
                throw new BusinessRuleException("Each invoice line must reference a product.");
            }
            Product product = productRepository.findById(line.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with ID: " + line.getProduct().getId()));
            line.setProduct(product);

            // Default description from product name if not specified
            if (line.getDescription() == null || line.getDescription().isBlank()) {
                line.setDescription(product.getName());
            }

            // Validate quantity
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException(
                        "Quantity must be positive for product: " + product.getSku());
            }

            // Default unit price from product base selling price if not specified
            if (line.getUnitPrice() == null || line.getUnitPrice().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal basePrice = product.getBaseSellingPrice();
                Long tierId = invoice.getCustomer() != null ? invoice.getCustomer().getPriceTierId() : null;
                
                if (tierId != null) {
                    BigDecimal discountFactor = BigDecimal.ONE;
                    if (tierId == 1) {
                        discountFactor = new BigDecimal("0.90");
                    } else if (tierId == 2) {
                        discountFactor = new BigDecimal("0.80");
                    } else if (tierId == 3) {
                        discountFactor = new BigDecimal("0.95");
                    }
                    line.setUnitPrice(basePrice.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP));
                } else {
                    line.setUnitPrice(basePrice);
                }
            }

            // Default discount to zero
            if (line.getDiscount() == null) {
                line.setDiscount(BigDecimal.ZERO);
            }
        }
    }

    /**
     * Validate that a status transition is permitted.
     */
    private void validateStatusTransition(InvoiceStatus current, InvoiceStatus target) {
        // Terminal states
        if (current == InvoiceStatus.CANCELLED || current == InvoiceStatus.VOIDED) {
            throw new BusinessRuleException(
                    "Cannot transition from terminal status: " + current);
        }
        if (current == InvoiceStatus.PAID && target != InvoiceStatus.VOIDED) {
            throw new BusinessRuleException(
                    "A PAID invoice can only be VOIDED, not transitioned to " + target);
        }

        // DRAFT may go to SENT or CANCELLED
        if (current == InvoiceStatus.DRAFT
                && target != InvoiceStatus.SENT
                && target != InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "DRAFT invoices can only be transitioned to SENT or CANCELLED, not " + target);
        }

        // SENT may go to PARTIALLY_PAID, PAID, OVERDUE, CANCELLED
        // PARTIALLY_PAID may go to PAID, OVERDUE, VOIDED
        // OVERDUE may go to PARTIALLY_PAID, PAID, CANCELLED, VOIDED
        // (other transitions are permissive within non-terminal states)
    }

    // ── Invoice Posting Transaction ─────────────────────────────────────

    @Override
    @Transactional
    @AuditAction(action = "POST", module = "BILLING")
    public Invoice postInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));

        // 1. Only DRAFT invoices can be posted
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT invoices can be posted. Current status: " + invoice.getStatus());
        }

        if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
            throw new BusinessRuleException("Cannot post an invoice with no line items.");
        }

        Customer customer = invoice.getCustomer();

        // 2. BILL-030: Allocate gap-free invoice number if not already present
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            String postedNumber = generateNextInvoiceNumber("");
            // Verify gap-free: must not already exist
            if (invoiceRepository.existsByInvoiceNumber(postedNumber)) {
                throw new BusinessRuleException(
                        "Gap-free invoice number collision: " + postedNumber + ". Please retry.");
            }
            invoice.setInvoiceNumber(postedNumber);
        }

        // 3. Customer credit checks
        if (customer.isCreditHold()) {
            throw new BusinessRuleException(
                    "Cannot post invoice: customer '" + customer.getName()
                    + "' is on credit hold.");
        }

        if (customer.getCreditLimit() != null) {
            BigDecimal currentOutstanding = invoiceRepository
                    .calculateOutstandingBalanceByCustomerId(customer.getId());
            BigDecimal projectedOutstanding = currentOutstanding.add(invoice.getGrandTotal());
            if (projectedOutstanding.compareTo(customer.getCreditLimit()) > 0) {
                throw new BusinessRuleException(
                        "Posting this invoice would exceed the customer's credit limit. "
                        + "Credit limit: " + customer.getCreditLimit()
                        + ", Current outstanding: " + currentOutstanding
                        + ", Invoice total: " + invoice.getGrandTotal()
                        + ", Projected: " + projectedOutstanding);
            }
        }

        // 4. BILL-050: Refresh line products
        for (InvoiceLine line : invoice.getLines()) {
            Product product = line.getProduct();
            final Long productId = product.getId();
            product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + productId));
            line.setProduct(product);
        }

        // Stock maintenance disabled - no stock decrement or inventory transaction audit saved.

        // 6. Generate balanced journal entry
        createInvoiceJournalEntry(invoice);

        // 7. Transition status DRAFT → SENT
        invoice.setStatus(InvoiceStatus.SENT);

        return invoiceRepository.save(invoice);
    }

    private void addJournalLine(JournalEntry je, Account account, BigDecimal debit, BigDecimal credit, String desc) {
        JournalLine jl = new JournalLine();
        jl.setAccount(account);
        jl.setDescription(desc);
        
        BigDecimal netDebit = debit.subtract(credit);
        if (netDebit.compareTo(BigDecimal.ZERO) >= 0) {
            jl.setDebitAmount(netDebit);
            jl.setCreditAmount(BigDecimal.ZERO);
        } else {
            jl.setDebitAmount(BigDecimal.ZERO);
            jl.setCreditAmount(netDebit.abs());
        }
        je.addLine(jl);
    }

    /**
     * Creates a balanced double-entry journal entry for a posted invoice or credit note:
     *
     * DR  Accounts Receivable       = grandTotal
     *   CR  Sales Revenue            = subtotal
     *   CR  Tax Payable (Output)     = taxTotal
     *
     * DR  Cost of Goods Sold         = Σ(line.qty × product.costPrice)
     *   CR  Inventory Asset          = Σ(line.qty × product.costPrice)
     */
    private void createInvoiceJournalEntry(Invoice invoice) {
        // Resolve mapped accounts
        Account arAccount = resolveAccount("ACCOUNTS_RECEIVABLE");
        Account salesAccount = resolveAccount("SALES_REVENUE");
        Account taxPayableAccount = resolveAccount("TAX_PAYABLE");
        Account cogsAccount = resolveAccount("COST_OF_GOODS_SOLD");
        Account inventoryAccount = resolveAccount("INVENTORY_ASSET");
        Account roundingAccount = resolveAccount("ROUNDING_ADJUSTMENT");

        JournalEntry je = new JournalEntry();
        je.setEntryDate(invoice.getInvoiceDate());
        je.setReferenceType("INVOICE");
        je.setReferenceId(invoice.getId());
        je.setReferenceNumber(invoice.getInvoiceNumber());
        je.setDescription("Invoice posting: " + invoice.getInvoiceNumber()
                + " — Customer: " + invoice.getCustomer().getName());

        // DR Accounts Receivable = grandTotal
        addJournalLine(je, arAccount, invoice.getGrandTotal(), BigDecimal.ZERO, "Accounts Receivable — " + invoice.getInvoiceNumber());

        // CR Sales Revenue = subtotal
        addJournalLine(je, salesAccount, BigDecimal.ZERO, invoice.getSubtotal(), "Sales Revenue — " + invoice.getInvoiceNumber());

        // CR Tax Payable = taxTotal (if != 0)
        if (invoice.getTaxTotal().compareTo(BigDecimal.ZERO) != 0) {
            addJournalLine(je, taxPayableAccount, BigDecimal.ZERO, invoice.getTaxTotal(), "Tax Payable — " + invoice.getInvoiceNumber());
        }

        // Rounding Adjustment
        BigDecimal roundOff = invoice.getRoundOff();
        if (roundOff != null && roundOff.compareTo(BigDecimal.ZERO) != 0) {
            if (roundOff.compareTo(BigDecimal.ZERO) < 0) {
                // Rounded down -> DR Rounding Adjustment
                addJournalLine(je, roundingAccount, roundOff.abs(), BigDecimal.ZERO, "Rounding Down — " + invoice.getInvoiceNumber());
            } else {
                // Rounded up -> CR Rounding Adjustment
                addJournalLine(je, roundingAccount, BigDecimal.ZERO, roundOff, "Rounding Up — " + invoice.getInvoiceNumber());
            }
        }

        // COGS / Inventory entries per line
        BigDecimal totalCogs = BigDecimal.ZERO;
        for (InvoiceLine line : invoice.getLines()) {
            BigDecimal lineCogs = line.getQuantity()
                    .multiply(line.getProduct().getCostPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            totalCogs = totalCogs.add(lineCogs);
        }

        if (totalCogs.compareTo(BigDecimal.ZERO) != 0) {
            addJournalLine(je, cogsAccount, totalCogs, BigDecimal.ZERO, "COGS — " + invoice.getInvoiceNumber());
            addJournalLine(je, inventoryAccount, BigDecimal.ZERO, totalCogs, "Inventory reduction — " + invoice.getInvoiceNumber());
        }

        // Validate balanced entry
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (JournalLine jel : je.getLines()) {
            totalDebits = totalDebits.add(jel.getDebitAmount());
            totalCredits = totalCredits.add(jel.getCreditAmount());
        }
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessRuleException(
                    "Journal entry is unbalanced! Debits: " + totalDebits
                    + ", Credits: " + totalCredits);
        }

        journalEntryRepository.save(je);
    }

    /**
     * Resolve a ledger account by its mapping key with auto-healing fallback.
     */
    private Account resolveAccount(String mappingKey) {
        return accountMappingRepository.findByMappingKey(mappingKey)
                .map(AccountMapping::getAccount)
                .orElseGet(() -> resolveAndSaveFallbackAccount(mappingKey));
    }

    private Account resolveAndSaveFallbackAccount(String mappingKey) {
        String defaultCode = switch (mappingKey) {
            case "CASH" -> "1010";
            case "BANK" -> "1020";
            case "ACCOUNTS_RECEIVABLE" -> "1100";
            case "INVENTORY_ASSET" -> "1200";
            case "TAX_RECEIVABLE" -> "1300";
            case "ACCOUNTS_PAYABLE" -> "2100";
            case "TAX_PAYABLE" -> "2200";
            case "SALES_REVENUE" -> "4000";
            case "COST_OF_GOODS_SOLD" -> "5000";
            default -> "5100";
        };

        com.businessmanager.backend.accounting.enums.AccountType targetType = switch (mappingKey) {
            case "CASH", "BANK", "ACCOUNTS_RECEIVABLE", "INVENTORY_ASSET", "TAX_RECEIVABLE" -> com.businessmanager.backend.accounting.enums.AccountType.ASSET;
            case "ACCOUNTS_PAYABLE", "TAX_PAYABLE", "ACCRUED_PURCHASES" -> com.businessmanager.backend.accounting.enums.AccountType.LIABILITY;
            case "SALES_REVENUE" -> com.businessmanager.backend.accounting.enums.AccountType.REVENUE;
            case "EQUITY" -> com.businessmanager.backend.accounting.enums.AccountType.EQUITY;
            default -> com.businessmanager.backend.accounting.enums.AccountType.EXPENSE;
        };

        Account fallback = accountRepository.findByCode(defaultCode)
                .or(() -> accountRepository.findAll().stream().filter(a -> a.getType() == targetType).findFirst())
                .or(() -> accountRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new BusinessRuleException(
                        "Account mapping not configured for key: " + mappingKey
                        + ". Please configure in account_mappings."));

        try {
            AccountMapping newMapping = new AccountMapping();
            newMapping.setMappingKey(mappingKey);
            newMapping.setAccount(fallback);
            newMapping.setCreatedBy("system");
            newMapping.setUpdatedBy("system");
            accountMappingRepository.save(newMapping);
        } catch (Exception ignored) {}

        return fallback;
    }
}
