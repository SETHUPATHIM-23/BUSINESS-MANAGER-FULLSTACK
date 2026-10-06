package com.businessmanager.backend.fund.controller;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.location.entity.Location;
import com.businessmanager.backend.fund.dto.*;
import com.businessmanager.backend.fund.entity.FundAccount;
import com.businessmanager.backend.fund.entity.FundAccountType;
import com.businessmanager.backend.fund.entity.FundTransaction;
import com.businessmanager.backend.fund.entity.FundTransactionType;
import com.businessmanager.backend.fund.entity.ReconciliationRecord;
import com.businessmanager.backend.fund.mapper.FundAccountMapper;
import com.businessmanager.backend.fund.mapper.FundTransactionMapper;
import com.businessmanager.backend.fund.mapper.ReconciliationRecordMapper;
import com.businessmanager.backend.fund.service.FundAccountService;
import com.businessmanager.backend.fund.service.FundTransactionService;
import com.businessmanager.backend.fund.service.ReconciliationRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/funds")
@RequiredArgsConstructor
public class FundController {

    private final FundAccountService fundAccountService;
    private final FundTransactionService fundTransactionService;
    private final ReconciliationRecordService reconciliationRecordService;

    private final FundAccountMapper fundAccountMapper;
    private final FundTransactionMapper fundTransactionMapper;
    private final ReconciliationRecordMapper reconciliationRecordMapper;

    // --- Fund Accounts ---

    @GetMapping("/accounts")
    @PreAuthorize("hasAuthority('FUND_READ')")
    public ResponseEntity<Page<FundAccountResponseDto>> searchAccounts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FundAccountType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<FundAccount> accounts = fundAccountService.searchFundAccounts(search, type, active, pageable);
        return ResponseEntity.ok(accounts.map(fundAccountMapper::toDto));
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<FundAccountResponseDto> createAccount(@Valid @RequestBody FundAccountCreateRequest request) {
        FundAccount account = fundAccountMapper.toEntity(request);
        
        if (request.getGlAccountId() != null) {
            Account glAccount = new Account();
            glAccount.setId(request.getGlAccountId());
            account.setGlAccount(glAccount);
        }
        
        if (request.getLocationId() != null) {
            Location location = new Location();
            location.setId(request.getLocationId());
            account.setLocation(location);
        }

        FundAccount saved = fundAccountService.createFundAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(fundAccountMapper.toDto(saved));
    }

    @GetMapping("/accounts/{id}")
    @PreAuthorize("hasAuthority('FUND_READ')")
    public ResponseEntity<FundAccountResponseDto> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(fundAccountMapper.toDto(fundAccountService.getFundAccountById(id)));
    }

    @PutMapping("/accounts/{id}")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<FundAccountResponseDto> updateAccount(@PathVariable Long id, @Valid @RequestBody FundAccountUpdateRequest request) {
        FundAccount account = new FundAccount();
        account.setName(request.getName());
        account.setType(request.getType());
        account.setAccountNumber(request.getAccountNumber());
        account.setActive(request.getActive());

        if (request.getGlAccountId() != null) {
            Account glAccount = new Account();
            glAccount.setId(request.getGlAccountId());
            account.setGlAccount(glAccount);
        }

        if (request.getLocationId() != null) {
            Location location = new Location();
            location.setId(request.getLocationId());
            account.setLocation(location);
        }

        FundAccount updated = fundAccountService.updateFundAccount(id, account);
        return ResponseEntity.ok(fundAccountMapper.toDto(updated));
    }

    @DeleteMapping("/accounts/{id}")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<Void> deactivateAccount(@PathVariable Long id) {
        fundAccountService.deactivateFundAccount(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/accounts/liquidity")
    @PreAuthorize("hasAuthority('FUND_READ')")
    public ResponseEntity<Map<String, BigDecimal>> getTotalLiquidity() {
        BigDecimal total = fundAccountService.getTotalLiquidity();
        return ResponseEntity.ok(Map.of("totalLiquidity", total != null ? total : BigDecimal.ZERO));
    }

    @GetMapping("/accounts/balance-by-type")
    @PreAuthorize("hasAuthority('FUND_READ')")
    public ResponseEntity<Map<String, BigDecimal>> getBalanceByType(@RequestParam FundAccountType type) {
        BigDecimal total = fundAccountService.getTotalBalanceByType(type);
        return ResponseEntity.ok(Map.of("balance", total != null ? total : BigDecimal.ZERO));
    }

    // --- Fund Transactions ---

    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('FUND_READ')")
    public ResponseEntity<Page<FundTransactionResponseDto>> searchTransactions(
            @RequestParam(required = false) Long fundAccountId,
            @RequestParam(required = false) FundTransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String refDocType,
            @RequestParam(required = false) Long refDocId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<FundTransaction> txs = fundTransactionService.searchTransactions(fundAccountId, type, startDate, endDate, refDocType, refDocId, pageable);
        return ResponseEntity.ok(txs.map(fundTransactionMapper::toDto));
    }

    @GetMapping("/transactions/{id}")
    @PreAuthorize("hasAuthority('FUND_READ')")
    public ResponseEntity<FundTransactionResponseDto> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(fundTransactionMapper.toDto(fundTransactionService.getTransactionById(id)));
    }

    @PostMapping("/transactions/transfer")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<FundTransactionResponseDto> transferFunds(@Valid @RequestBody FundTransferRequest request) {
        FundTransaction tx = fundTransactionService.transferFunds(
                request.getSourceAccountId(),
                request.getTargetAccountId(),
                request.getAmount(),
                request.getTransactionDate(),
                request.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(fundTransactionMapper.toDto(tx));
    }

    @PostMapping("/transactions/customer-payment")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<FundTransactionResponseDto> recordCustomerPayment(@Valid @RequestBody CustomerPaymentFundRequest request) {
        String desc = request.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = "Customer payment credited to fund account";
        }
        FundTransaction tx = fundTransactionService.recordAutomaticTransaction(
                request.getFundAccountId(),
                FundTransactionType.RECEIPT,
                request.getAmount(),
                request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now(),
                "CUSTOMER",
                request.getCustomerId() != null ? request.getCustomerId() : request.getInvoiceId(),
                desc
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(fundTransactionMapper.toDto(tx));
    }

    @PostMapping("/transactions/supplier-settlement")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<FundTransactionResponseDto> recordSupplierSettlement(@Valid @RequestBody SupplierSettlementFundRequest request) {
        String desc = request.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = "Supplier payment settled from fund account";
        }
        FundTransaction tx = fundTransactionService.recordAutomaticTransaction(
                request.getFundAccountId(),
                FundTransactionType.PAYMENT,
                request.getAmount(),
                request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now(),
                "SUPPLIER",
                request.getSupplierId() != null ? request.getSupplierId() : request.getPurchaseOrderId(),
                desc
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(fundTransactionMapper.toDto(tx));
    }

    @PostMapping("/transactions/{id}/reverse")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<FundTransactionResponseDto> reverseTransaction(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        FundTransaction tx = fundTransactionService.reverseTransaction(id, reason);
        return ResponseEntity.ok(fundTransactionMapper.toDto(tx));
    }

    @GetMapping("/transactions/summary")
    @PreAuthorize("hasAuthority('FUND_READ')")
    public ResponseEntity<Map<String, BigDecimal>> getTransactionSummary() {
        BigDecimal totalReceived = fundTransactionService.getTotalReceipts();
        BigDecimal totalSent = fundTransactionService.getTotalPayments();
        return ResponseEntity.ok(Map.of(
                "totalReceived", totalReceived != null ? totalReceived : BigDecimal.ZERO,
                "totalSent", totalSent != null ? totalSent : BigDecimal.ZERO
        ));
    }

    @PostMapping("/transactions/record")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<FundTransactionResponseDto> recordDirectTransaction(@Valid @RequestBody FundTransactionRequest request) {
        FundTransaction tx = fundTransactionService.recordAutomaticTransaction(
                request.getFundAccountId(),
                request.getType(),
                request.getAmount(),
                request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now(),
                request.getReferenceDocumentType() != null ? request.getReferenceDocumentType() : "MANUAL",
                request.getReferenceDocumentId(),
                request.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(fundTransactionMapper.toDto(tx));
    }
    // --- Reconciliation Records ---

    @GetMapping("/reconciliations")
    @PreAuthorize("hasAuthority('FUND_READ')")
    public ResponseEntity<Page<ReconciliationResponseDto>> searchReconciliations(
            @RequestParam(required = false) Long fundAccountId,
            @RequestParam(required = false) Boolean matched,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ReconciliationRecord> recs = reconciliationRecordService.searchReconciliationRecords(fundAccountId, matched, search, pageable);
        return ResponseEntity.ok(recs.map(reconciliationRecordMapper::toDto));
    }

    @PostMapping("/reconciliations")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<ReconciliationResponseDto> createReconciliationRecord(@Valid @RequestBody ReconciliationRequest request) {
        ReconciliationRecord record = reconciliationRecordService.createReconciliationRecord(
                request.getFundTransactionId(),
                request.getBankStatementLineRef()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(reconciliationRecordMapper.toDto(record));
    }

    @PostMapping("/reconciliations/{id}/match")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<ReconciliationResponseDto> matchReconciliation(
            @PathVariable Long id,
            Principal principal) {
        String username = principal != null ? principal.getName() : "SYSTEM";
        ReconciliationRecord record = reconciliationRecordService.matchReconciliationRecord(id, username);
        return ResponseEntity.ok(reconciliationRecordMapper.toDto(record));
    }

    @DeleteMapping("/reconciliations/{id}")
    @PreAuthorize("hasAuthority('FUND_WRITE')")
    public ResponseEntity<Void> deleteReconciliation(@PathVariable Long id) {
        reconciliationRecordService.deleteReconciliationRecord(id);
        return ResponseEntity.noContent().build();
    }
}
