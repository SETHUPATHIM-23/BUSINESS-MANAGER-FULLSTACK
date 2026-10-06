package com.businessmanager.backend.billing.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InvoiceService {

    /**
     * Create a new invoice in DRAFT status with its line items.
     * Generates a unique invoice number automatically.
     */
    Invoice createInvoice(Invoice invoice);

    /**
     * Retrieve a single invoice by primary key (with lines eager-loaded).
     */
    Invoice getInvoiceById(Long id);

    /**
     * Retrieve a single invoice by its unique invoice number.
     */
    Invoice getInvoiceByNumber(String invoiceNumber);

    /**
     * Filtered and paginated search across invoices.
     */
    Page<Invoice> searchInvoices(String search, Long customerId, InvoiceStatus status,
                                 LocalDate dateFrom, LocalDate dateTo, Pageable pageable);

    /**
     * Update an existing invoice (header + lines).
     * Only DRAFT invoices may be edited.
     */
    Invoice updateInvoice(Long id, Invoice updated);

    /**
     * Transition an invoice to a new status (SENT, PAID, CANCELLED, etc.).
     */
    Invoice changeStatus(Long id, InvoiceStatus newStatus);

    /**
     * Cancel (soft-delete) an invoice. Invoices with payments cannot be deleted,
     * only cancelled/voided.
     */
    void cancelInvoice(Long id);

    /**
     * Hard-delete an invoice. Only DRAFT invoices with zero amount_paid
     * may be hard-deleted.
     */
    void deleteInvoice(Long id);

    /**
     * Record a payment against an invoice, updating amount_paid and status.
     */
    Invoice recordPayment(Long id, Long paymentAccountId, BigDecimal amount);

    /**
     * Calculate total outstanding balance for a customer.
     */
    BigDecimal getCustomerOutstandingBalance(Long customerId);

    /**
     * Calculate total revenue across all invoices.
     */
    BigDecimal getTotalRevenue();

    /**
     * Calculate total outstanding balance across all invoices.
     */
    BigDecimal getTotalOutstandingBalance();

    /**
     * Generate the next sequential invoice number with the given prefix.
     */
    String generateNextInvoiceNumber(String prefix);

    /**
     * Post a DRAFT invoice as a single atomic transaction:
     * 1. Allocate gap-free invoice number (BILL-030)
     * 2. Check customer credit rules (credit hold + credit limit)
     * 3. Check stock availability per line; block if insufficient and negative stock disabled (BILL-050)
     * 4. Decrement inventory for each line (BILL-040)
     * 5. Generate balanced journal entry (DR Accounts Receivable / CR Sales Revenue + Tax Payable,
     *    DR COGS / CR Inventory Asset)
     *
     * Any failure at any step rolls back everything.
     */
    Invoice postInvoice(Long id);

    /**
     * Atomic POS-style checkout: Posts the invoice and immediately applies the payment (BILL-060).
     */
    Invoice postAndPayInvoice(Long id, Long paymentAccountId, BigDecimal amount);

    /**
     * Processes a return (BILL-070):
     * 1. Validates the return quantities.
     * 2. Generates a new Credit Note invoice.
     * 3. Restores inventory for the returned products.
     * 4. Generates a reversing journal entry to reduce AR, Sales, and Taxes.
     */
    Invoice processReturn(Long originalInvoiceId, java.util.Map<Long, BigDecimal> returnedQuantities);

    /**
     * Preview totals for an unsaved invoice draft.
     * Computes line item calculations, taxes, rounding, subtotal, and grand total.
     */
    Invoice previewTotals(Invoice invoice);
}
