package com.businessmanager.backend.billing.controller;

import com.businessmanager.backend.billing.dto.BillingSummaryDto;
import com.businessmanager.backend.billing.dto.InvoiceCreateRequest;
import com.businessmanager.backend.billing.dto.InvoiceLineResponseDto;
import com.businessmanager.backend.billing.dto.InvoiceResponseDto;
import com.businessmanager.backend.billing.dto.InvoiceSummaryDto;
import com.businessmanager.backend.billing.dto.InvoiceUpdateRequest;
import com.businessmanager.backend.billing.dto.ProcessReturnRequest;
import com.businessmanager.backend.billing.dto.RecordPaymentRequest;
import com.businessmanager.backend.billing.dto.StatusChangeRequest;
import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.entity.InvoiceLine;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import com.businessmanager.backend.billing.mapper.InvoiceMapper;
import com.businessmanager.backend.billing.repository.InvoiceLineRepository;
import com.businessmanager.backend.billing.service.InvoiceService;

import com.businessmanager.backend.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for the Billing module – Invoices and Invoice Lines.
 *
 * <p>Base path: {@code /api/billings}
 *
 * <p>RBAC summary (authority-based, enforced via {@code @EnableMethodSecurity}):
 * <ul>
 *   <li>{@code BILLING_READ}  – Accountant, Administrator</li>
 *   <li>{@code BILLING_WRITE} – Sales/Billing Staff, Administrator</li>
 *   <li>{@code BILLING_POST}  – Administrator (invoice posting triggers journal entries)</li>
 *   <li>{@code BILLING_DELETE}– Administrator only</li>
 * </ul>
 *
 * <p>Error handling: all exceptions thrown by the service layer (
 * {@link com.businessmanager.backend.common.exception.ResourceNotFoundException},
 * {@link com.businessmanager.backend.common.exception.BusinessRuleException},
 * {@link org.springframework.security.core.AuthenticationException},
 * and any unhandled {@link Exception}) are caught by
 * {@link com.businessmanager.backend.common.exception.GlobalExceptionHandler}
 * and mapped to structured {@link com.businessmanager.backend.common.exception.ErrorResponse}
 * JSON bodies with the appropriate HTTP status codes (400/401/404/409/500).
 *
 * <p>Traces to: BILL-010 (Invoice management), BILL-020 (Invoice line management).
 */
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/billings")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Billing and Invoicing Management")
public class BillingController {

    private final InvoiceService invoiceService;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceMapper invoiceMapper;


    // ═══════════════════════════════════════════════════════════════════
    // INVOICE CRUD
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Create a new Invoice in DRAFT status.
     *
     * <p>Roles: Sales/Billing Staff, Administrator.
     *
     * @param request validated invoice creation payload
     * @return 201 Created with the new invoice body
     */
    @PostMapping
    @Operation(summary = "Create a new Invoice in DRAFT status")
    @PreAuthorize("hasAuthority('BILLING_WRITE')")
    public ResponseEntity<InvoiceResponseDto> createInvoice(
            @Valid @RequestBody InvoiceCreateRequest request) {

        Invoice entity = invoiceMapper.toEntity(request);
        Invoice saved = invoiceService.createInvoice(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceMapper.toResponseDto(saved));
    }

    /**
     * Preview totals for an unsaved DRAFT invoice (billingex flow).
     *
     * <p>Roles: Sales/Billing Staff, Administrator.
     *
     * @param request validated invoice creation payload
     * @return 200 OK with the calculated invoice body (not saved)
     */
    @PostMapping("/preview-totals")
    @Operation(summary = "Preview totals for an unsaved DRAFT invoice (billingex flow)")
    @PreAuthorize("hasAuthority('BILLING_WRITE')")
    public ResponseEntity<InvoiceResponseDto> previewTotals(
            @Valid @RequestBody InvoiceCreateRequest request) {

        Invoice entity = invoiceMapper.toEntity(request);
        Invoice preview = invoiceService.previewTotals(entity);
        return ResponseEntity.ok(invoiceMapper.toResponseDto(preview));
    }

    /**
     * Retrieve a single invoice by its primary-key ID.
     *
     * <p>Roles: Accountant, Administrator, Sales/Billing Staff (BILLING_READ).
     *
     * @param id invoice primary key
     * @return 200 with full invoice body, or 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a single invoice by its primary-key ID")
    @PreAuthorize("hasAuthority('BILLING_READ')")
    public ResponseEntity<InvoiceResponseDto> getInvoiceById(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoiceMapper.toResponseDto(invoice));
    }

    /**
     * Retrieve a single invoice by its human-readable invoice number.
     *
     * <p>Roles: Accountant, Administrator, Sales/Billing Staff.
     *
     * @param invoiceNumber e.g. "INV-000042"
     * @return 200 with full invoice body, or 404 if not found
     */
    @GetMapping("/number/{invoiceNumber}")
    @Operation(summary = "Retrieve a single invoice by its human-readable invoice number")
    @PreAuthorize("hasAuthority('BILLING_READ')")
    public ResponseEntity<InvoiceResponseDto> getInvoiceByNumber(
            @PathVariable String invoiceNumber) {

        Invoice invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
        return ResponseEntity.ok(invoiceMapper.toResponseDto(invoice));
    }

    /**
     * Paginated, filtered invoice list.
     *
     * <p>Roles: Accountant, Administrator, Sales/Billing Staff.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code search}     – free-text search on invoice number / customer name</li>
     *   <li>{@code customerId} – filter by customer</li>
     *   <li>{@code status}     – filter by {@link InvoiceStatus}</li>
     *   <li>{@code dateFrom}   – lower bound on invoice date (ISO-8601)</li>
     *   <li>{@code dateTo}     – upper bound on invoice date (ISO-8601)</li>
     *   <li>standard {@code page}, {@code size}, {@code sort} Pageable parameters</li>
     * </ul>
     *
     * @return 200 with paginated summary list
     */
    @GetMapping
    @Operation(summary = "Search and paginate invoices")
    @PreAuthorize("hasAuthority('BILLING_READ')")
    public ResponseEntity<PageResponse<InvoiceSummaryDto>> searchInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Invoice> page = invoiceService.searchInvoices(
                search, customerId, status, dateFrom, dateTo, pageable);

        List<InvoiceSummaryDto> dtos = invoiceMapper.toSummaryDtoList(page.getContent());
        return ResponseEntity.ok(new PageResponse<>(dtos, page));
    }

    /**
     * Update an existing DRAFT invoice (header and/or lines).
     *
     * <p>Only DRAFT invoices can be edited; the service throws
     * {@link com.businessmanager.backend.common.exception.BusinessRuleException}
     * (→ HTTP 409) for any other status.
     *
     * <p>Roles: Sales/Billing Staff, Administrator.
     *
     * @param id      invoice primary key
     * @param request validated update payload
     * @return 200 with updated invoice body
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing DRAFT invoice")
    @PreAuthorize("hasAuthority('BILLING_WRITE')")
    public ResponseEntity<InvoiceResponseDto> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceUpdateRequest request) {

        Invoice overlay = invoiceMapper.toEntity(request);
        Invoice updated = invoiceService.updateInvoice(id, overlay);
        return ResponseEntity.ok(invoiceMapper.toResponseDto(updated));
    }

    /**
     * Hard-delete a DRAFT invoice with zero payments.
     *
     * <p>Roles: Administrator only. Non-DRAFT or paid invoices must be cancelled
     * via {@code PATCH /{id}/cancel} instead.
     *
     * @param id invoice primary key
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Hard-delete a DRAFT invoice")
    @PreAuthorize("hasAuthority('BILLING_DELETE')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // INVOICE ACTION ENDPOINTS (module-specific)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Post a DRAFT invoice as a single atomic transaction (BILL-030/040/050).
     *
     * <p>This is the most privileged write operation; it:
     * <ol>
     *   <li>Allocates a gap-free sequential invoice number</li>
     *   <li>Checks customer credit hold + credit limit</li>
     *   <li>Validates stock availability per line (blocks if negative stock disabled)</li>
     *   <li>Decrements inventory for each line</li>
     *   <li>Generates a balanced double-entry journal entry (AR / Sales / Tax / COGS / Inventory)</li>
     *   <li>Transitions status DRAFT → SENT</li>
     * </ol>
     *
     * <p>Roles: Administrator only (irreversible financial write).
     *
     * @param id invoice primary key
     * @return 200 with the posted (SENT) invoice body
     */
    @PostMapping("/{id}/post")
    @Operation(summary = "Post a DRAFT invoice (integrate with inventory/accounting)")
    @PreAuthorize("hasAuthority('BILLING_POST')")
    public ResponseEntity<InvoiceResponseDto> postInvoice(@PathVariable Long id) {
        Invoice posted = invoiceService.postInvoice(id);
        return ResponseEntity.ok(invoiceMapper.toResponseDto(posted));
    }

    /**
     * Transition an invoice to a new status (e.g. SENT → OVERDUE, DRAFT → CANCELLED).
     *
     * <p>Valid transitions are enforced by the service; invalid ones throw
     * {@link com.businessmanager.backend.common.exception.BusinessRuleException} (→ 409).
     *
     * <p>Roles: Administrator (full), Sales/Billing Staff (limited transitions via BILLING_WRITE).
     *
     * @param id      invoice primary key
     * @param request body containing the target {@link InvoiceStatus}
     * @return 200 with updated invoice body
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Change invoice status")
    @PreAuthorize("hasAuthority('BILLING_WRITE')")
    public ResponseEntity<InvoiceResponseDto> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request) {

        Invoice updated = invoiceService.changeStatus(id, request.getStatus());
        return ResponseEntity.ok(invoiceMapper.toResponseDto(updated));
    }

    /**
     * Cancel (or void) an invoice.
     *
     * <p>Invoices with payments are automatically voided instead of cancelled;
     * both transitions are handled by the service layer.
     *
     * <p>Roles: Administrator only — cancellations have financial implications.
     *
     * @param id invoice primary key
     * @return 200 OK (idempotent; body is empty)
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel or void an invoice")
    @PreAuthorize("hasAuthority('BILLING_DELETE')")
    public ResponseEntity<Void> cancelInvoice(@PathVariable Long id) {
        invoiceService.cancelInvoice(id);
        return ResponseEntity.ok().build();
    }


    /**
     * Record a partial or full payment against an invoice.
     *
     * <p>Auto-transitions status to PARTIALLY_PAID or PAID based on the
     * cumulative amount_paid vs grand_total.
     *
     * <p>Roles: Administrator, Sales/Billing Staff.
     *
     * @param id      invoice primary key
     * @param request payment amount
     * @return 200 with updated invoice body reflecting new payment status
     */
    @PostMapping("/{id}/payments")
    @Operation(summary = "Record payment against invoice")
    @PreAuthorize("hasAuthority('BILLING_WRITE')")
    public ResponseEntity<InvoiceResponseDto> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody RecordPaymentRequest request) {

        Invoice updated = invoiceService.recordPayment(id, request.getPaymentAccountId(), request.getAmount());
        return ResponseEntity.ok(invoiceMapper.toResponseDto(updated));
    }

    /**
     * POST /api/billing/invoices/{id}/post-and-pay
     *
     * @param id The invoice ID
     * @param request The payment details
     * @return 200 with updated invoice body reflecting POSTED and PAID status
     */
    @PostMapping("/{id}/post-and-pay")
    @Operation(summary = "Atomically post and apply full/partial payment")
    @PreAuthorize("hasAuthority('BILLING_POST')")
    public ResponseEntity<InvoiceResponseDto> postAndPayInvoice(
            @PathVariable Long id,
            @Valid @RequestBody RecordPaymentRequest request) {

        Invoice updated = invoiceService.postAndPayInvoice(id, request.getPaymentAccountId(), request.getAmount());
        return ResponseEntity.ok(invoiceMapper.toResponseDto(updated));
    }

    /**
     * POST /api/billing/invoices/{id}/return
     *
     * @param id The original invoice ID
     * @param request Map of line ID to return quantity
     * @return 201 Created with the generated Credit Note invoice
     */
    @PostMapping("/{id}/return")
    @Operation(summary = "Process return and generate credit note")
    @PreAuthorize("hasAuthority('BILLING_WRITE')")
    public ResponseEntity<InvoiceResponseDto> processReturn(
            @PathVariable Long id,
            @Valid @RequestBody ProcessReturnRequest request) {

        Invoice creditNote = invoiceService.processReturn(id, request.getReturnedQuantities());
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceMapper.toResponseDto(creditNote));
    }

    // ═══════════════════════════════════════════════════════════════════
    // INVOICE LINE SUB-RESOURCE
    // ═══════════════════════════════════════════════════════════════════

    /**
     * List all line items for a specific invoice.
     *
     * <p>Roles: Accountant, Administrator, Sales/Billing Staff.
     *
     * @param invoiceId invoice primary key
     * @return 200 with ordered list of line-item DTOs
     */
    @GetMapping("/{invoiceId}/lines")
    @Operation(summary = "List all line items for an invoice")
    @PreAuthorize("hasAuthority('BILLING_READ')")
    public ResponseEntity<List<InvoiceLineResponseDto>> getLinesForInvoice(
            @PathVariable Long invoiceId) {

        // Ensure the parent invoice exists (throws 404 via GlobalExceptionHandler if not)
        invoiceService.getInvoiceById(invoiceId);

        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceIdOrderByIdAsc(invoiceId);
        return ResponseEntity.ok(invoiceMapper.toLineResponseDtoList(lines));
    }

    // ═══════════════════════════════════════════════════════════════════
    // AGGREGATE / SPECIAL-VIEW ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Total revenue across all non-cancelled/voided invoices.
     *
     * <p>Roles: Accountant, Administrator.
     *
     * @return 200 with {@link BillingSummaryDto} containing the total revenue figure
     */
    @GetMapping("/summary/revenue")
    @Operation(summary = "Get total revenue across all active invoices")
    @PreAuthorize("hasAuthority('BILLING_READ')")
    public ResponseEntity<BillingSummaryDto> getTotalRevenue() {
        return ResponseEntity.ok(
                BillingSummaryDto.of("Total Revenue", invoiceService.getTotalRevenue()));
    }

    /**
     * Total outstanding balance (grand_total − amount_paid) across all active invoices.
     *
     * <p>Roles: Accountant, Administrator.
     *
     * @return 200 with {@link BillingSummaryDto} containing the outstanding balance
     */
    @GetMapping("/summary/outstanding")
    @Operation(summary = "Get total outstanding balance across all invoices")
    @PreAuthorize("hasAuthority('BILLING_READ')")
    public ResponseEntity<BillingSummaryDto> getTotalOutstanding() {
        return ResponseEntity.ok(
                BillingSummaryDto.of("Total Outstanding Balance",
                        invoiceService.getTotalOutstandingBalance()));
    }

    /**
     * Outstanding balance for a specific customer.
     *
     * <p>Used by the Billing special-view panel to show per-customer AR exposure.
     *
     * <p>Roles: Accountant, Administrator, Sales/Billing Staff.
     *
     * @param customerId customer primary key
     * @return 200 with {@link BillingSummaryDto} containing the customer outstanding figure
     */
    @GetMapping("/customers/{customerId}/outstanding")
    @Operation(summary = "Get outstanding balance for a specific customer")
    @PreAuthorize("hasAuthority('BILLING_READ')")
    public ResponseEntity<BillingSummaryDto> getCustomerOutstanding(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                BillingSummaryDto.of("Customer Outstanding Balance",
                        invoiceService.getCustomerOutstandingBalance(customerId)));
    }

    /**
     * Preview the next invoice number that would be auto-generated.
     *
     * <p>Used by the frontend invoice-creation form to show the upcoming number
     * in read-only mode before the user submits.
     *
     * <p>Roles: Sales/Billing Staff, Administrator.
     *
     * @param prefix optional prefix (defaults to "INV-")
     * @return 200 with the next invoice number string
     */
    @GetMapping("/next-number")
    @Operation(summary = "Preview the next invoice number")
    @PreAuthorize("hasAuthority('BILLING_WRITE')")
    public ResponseEntity<String> previewNextInvoiceNumber(
            @RequestParam(defaultValue = "") String prefix) {

        return ResponseEntity.ok(invoiceService.generateNextInvoiceNumber(prefix));
    }
}
