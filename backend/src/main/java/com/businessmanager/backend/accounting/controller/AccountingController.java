package com.businessmanager.backend.accounting.controller;

import com.businessmanager.backend.accounting.dto.*;
import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.FiscalPeriod;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.entity.JournalLine;
import com.businessmanager.backend.accounting.enums.AccountType;
import com.businessmanager.backend.accounting.mapper.AccountMapper;
import com.businessmanager.backend.accounting.mapper.FiscalPeriodMapper;
import com.businessmanager.backend.accounting.mapper.JournalEntryMapper;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.repository.FiscalPeriodRepository;
import com.businessmanager.backend.accounting.repository.JournalLineRepository;
import com.businessmanager.backend.accounting.service.AccountService;
import com.businessmanager.backend.accounting.service.JournalEntryService;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/accountings")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountService accountService;
    private final JournalEntryService journalEntryService;
    private final AccountRepository accountRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JournalLineRepository journalLineRepository;
    
    private final AccountMapper accountMapper;
    private final JournalEntryMapper journalEntryMapper;
    private final FiscalPeriodMapper fiscalPeriodMapper;

    // ── CHART OF ACCOUNTS (CRUD) ──────────────────────────────────────────

    @PostMapping("/accounts")
    @PreAuthorize("hasAuthority('ACCOUNTING_WRITE')")
    public ResponseEntity<AccountResponseDto> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        Account account = accountMapper.toEntity(request);
        if (request.getParentId() != null) {
            account.setParent(accountService.getAccountById(request.getParentId()));
        }
        Account savedAccount = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponseDto(savedAccount));
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAuthority('ACCOUNTING_READ')")
    public ResponseEntity<Page<AccountResponseDto>> searchAccounts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Account> accountsPage = accountService.searchAccounts(search, type, pageable);
        
        Page<AccountResponseDto> dtoPage = accountsPage.map(this::convertToResponseDto);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/accounts/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING_READ')")
    public ResponseEntity<AccountResponseDto> getAccountById(@PathVariable Long id) {
        Account account = accountService.getAccountById(id);
        return ResponseEntity.ok(convertToResponseDto(account));
    }

    @PutMapping("/accounts/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING_WRITE')")
    public ResponseEntity<AccountResponseDto> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountUpdateRequest request) {
        
        Account accountDetails = accountMapper.toEntity(request);
        if (request.getParentId() != null) {
            accountDetails.setParent(accountService.getAccountById(request.getParentId()));
        }
        Account updatedAccount = accountService.updateAccount(id, accountDetails);
        return ResponseEntity.ok(convertToResponseDto(updatedAccount));
    }

    @PostMapping("/accounts/{id}/deactivate")
    @PreAuthorize("hasAuthority('ACCOUNTING_WRITE')")
    public ResponseEntity<Void> deactivateAccount(@PathVariable Long id) {
        accountService.deactivateAccount(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/accounts/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING_WRITE')")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    // ── JOURNAL ENTRIES & BALANCED POSTING ────────────────────────────────

    @PostMapping("/entries")
    @PreAuthorize("hasAuthority('ACCOUNTING_WRITE')")
    public ResponseEntity<JournalEntryResponseDto> postJournalEntry(@Valid @RequestBody JournalEntryCreateRequest request) {
        JournalEntry entry = journalEntryMapper.toEntity(request);
        
        // Resolve accounts for each line
        if (entry.getLines() != null && request.getLines() != null) {
            for (int i = 0; i < entry.getLines().size(); i++) {
                Long accountId = request.getLines().get(i).getAccountId();
                entry.getLines().get(i).setAccount(accountService.getAccountById(accountId));
            }
        }
        
        JournalEntry posted = journalEntryService.postJournalEntry(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(journalEntryMapper.toResponseDto(posted));
    }

    @GetMapping("/entries")
    @PreAuthorize("hasAuthority('ACCOUNTING_READ')")
    public ResponseEntity<Page<JournalEntrySummaryDto>> searchJournalEntries(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String sourceModule,
            @RequestParam(required = false) Long fiscalPeriodId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "entryDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<JournalEntry> entries = journalEntryService.searchJournalEntries(search, startDate, endDate, sourceModule, fiscalPeriodId, pageable);
        
        return ResponseEntity.ok(entries.map(journalEntryMapper::toSummaryDto));
    }

    @GetMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('ACCOUNTING_READ')")
    public ResponseEntity<JournalEntryResponseDto> getJournalEntryById(@PathVariable Long id) {
        JournalEntry entry = journalEntryService.getJournalEntryById(id);
        return ResponseEntity.ok(journalEntryMapper.toResponseDto(entry));
    }

    // ── FISCAL PERIOD MANAGEMENT ──────────────────────────────────────────

    @PostMapping("/periods")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<FiscalPeriodResponseDto> createFiscalPeriod(@Valid @RequestBody FiscalPeriodRequest request) {
        FiscalPeriod period = fiscalPeriodMapper.toEntity(request);
        FiscalPeriod saved = journalEntryService.createFiscalPeriod(period);
        return ResponseEntity.status(HttpStatus.CREATED).body(fiscalPeriodMapper.toResponseDto(saved));
    }

    @PostMapping("/periods/{id}/close")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<FiscalPeriodResponseDto> closePeriod(@PathVariable Long id) {
        FiscalPeriod closed = journalEntryService.closeFiscalPeriod(id);
        return ResponseEntity.ok(fiscalPeriodMapper.toResponseDto(closed));
    }

    @PostMapping("/periods/{id}/reopen")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<FiscalPeriodResponseDto> reopenPeriod(@PathVariable Long id) {
        FiscalPeriod reopened = journalEntryService.reopenFiscalPeriod(id);
        return ResponseEntity.ok(fiscalPeriodMapper.toResponseDto(reopened));
    }

    @GetMapping("/periods")
    @PreAuthorize("hasAuthority('ACCOUNTING_READ')")
    public ResponseEntity<List<FiscalPeriodResponseDto>> getAllPeriods() {
        List<FiscalPeriod> periods = fiscalPeriodRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate"));
        return ResponseEntity.ok(fiscalPeriodMapper.toResponseDtoList(periods));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    private AccountResponseDto convertToResponseDto(Account account) {
        AccountResponseDto dto = accountMapper.toResponseDto(account);
        BigDecimal debits = accountRepository.getTotalDebits(account.getId());
        BigDecimal credits = accountRepository.getTotalCredits(account.getId());
        
        dto.setDebitTotal(debits);
        dto.setCreditTotal(credits);

        if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
            dto.setNetBalance(debits.subtract(credits));
        } else {
            dto.setNetBalance(credits.subtract(debits));
        }
        return dto;
    }

    // ── REPORTING ENDPOINTS (ACCT-060, ACCT-070) ───────────────────────────

    private void resolveDateRange(LocalDate startDate, LocalDate endDate, Long fiscalPeriodId, LocalDate[] resolved) {
        LocalDate start = startDate;
        LocalDate end = endDate;
        if (fiscalPeriodId != null) {
            FiscalPeriod fp = fiscalPeriodRepository.findById(fiscalPeriodId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fiscal period not found with ID: " + fiscalPeriodId));
            start = fp.getStartDate();
            end = fp.getEndDate();
        }
        if (start == null) {
            start = LocalDate.of(2000, 1, 1);
        }
        if (end == null) {
            end = LocalDate.now();
        }
        resolved[0] = start;
        resolved[1] = end;
    }

    @GetMapping("/reports/trial-balance")
    @PreAuthorize("hasAuthority('ACCOUNTING_READ')")
    public ResponseEntity<List<ReportDtos.TrialBalanceItem>> getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long fiscalPeriodId) {

        LocalDate[] range = new LocalDate[2];
        resolveDateRange(startDate, endDate, fiscalPeriodId, range);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<Account> accounts = accountRepository.findAll();
        List<ReportDtos.TrialBalanceItem> items = new ArrayList<>();

        for (Account acc : accounts) {
            BigDecimal debits = journalLineRepository.getDebitsForPeriod(acc.getId(), start, end);
            BigDecimal credits = journalLineRepository.getCreditsForPeriod(acc.getId(), start, end);
            BigDecimal net;
            if (acc.getType() == AccountType.ASSET || acc.getType() == AccountType.EXPENSE) {
                net = debits.subtract(credits);
            } else {
                net = credits.subtract(debits);
            }

            items.add(ReportDtos.TrialBalanceItem.builder()
                    .accountId(acc.getId())
                    .code(acc.getCode())
                    .name(acc.getName())
                    .type(acc.getType().name())
                    .debitTotal(debits)
                    .creditTotal(credits)
                    .netBalance(net)
                    .build());
        }

        return ResponseEntity.ok(items);
    }

    @GetMapping("/reports/general-ledger")
    @PreAuthorize("hasAuthority('ACCOUNTING_READ')")
    public ResponseEntity<List<ReportDtos.GeneralLedgerDetail>> getGeneralLedger(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long fiscalPeriodId) {

        LocalDate[] range = new LocalDate[2];
        resolveDateRange(startDate, endDate, fiscalPeriodId, range);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<Account> targetAccounts = new ArrayList<>();
        if (accountId != null) {
            targetAccounts.add(accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId)));
        } else {
            targetAccounts.addAll(accountRepository.findAll());
        }

        List<ReportDtos.GeneralLedgerDetail> details = new ArrayList<>();

        for (Account acc : targetAccounts) {
            BigDecimal openBal = journalLineRepository.getOpeningBalance(acc.getId(), start);
            List<JournalLine> jlList = journalLineRepository.findByAccountIdAndDateRange(acc.getId(), start, end);
            
            List<ReportDtos.GeneralLedgerLine> glLines = new ArrayList<>();
            BigDecimal runBal = openBal;

            for (JournalLine jl : jlList) {
                BigDecimal deb = jl.getDebitAmount();
                BigDecimal cred = jl.getCreditAmount();
                if (acc.getType() == AccountType.ASSET || acc.getType() == AccountType.EXPENSE) {
                    runBal = runBal.add(deb).subtract(cred);
                } else {
                    runBal = runBal.add(cred).subtract(deb);
                }

                glLines.add(ReportDtos.GeneralLedgerLine.builder()
                        .entryDate(jl.getJournalEntry().getEntryDate())
                        .reference(jl.getJournalEntry().getReference())
                        .memo(jl.getDescription() != null ? jl.getDescription() : jl.getJournalEntry().getMemo())
                        .debitAmount(deb)
                        .creditAmount(cred)
                        .runningBalance(runBal)
                        .build());
            }

            details.add(ReportDtos.GeneralLedgerDetail.builder()
                    .accountId(acc.getId())
                    .code(acc.getCode())
                    .name(acc.getName())
                    .type(acc.getType().name())
                    .openingBalance(openBal)
                    .lines(glLines)
                    .closingBalance(runBal)
                    .build());
        }

        return ResponseEntity.ok(details);
    }

    @GetMapping("/reports/profit-loss")
    @PreAuthorize("hasAuthority('ACCOUNTING_READ')")
    public ResponseEntity<ReportDtos.ProfitAndLossReport> getProfitAndLoss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long fiscalPeriodId) {

        LocalDate[] range = new LocalDate[2];
        resolveDateRange(startDate, endDate, fiscalPeriodId, range);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<Account> accounts = accountRepository.findAll();
        List<ReportDtos.ReportAccountItem> revenues = new ArrayList<>();
        List<ReportDtos.ReportAccountItem> expenses = new ArrayList<>();

        BigDecimal totalRev = BigDecimal.ZERO;
        BigDecimal totalExp = BigDecimal.ZERO;

        for (Account acc : accounts) {
            if (acc.getType() == AccountType.INCOME || acc.getType() == AccountType.EXPENSE) {
                BigDecimal debits = journalLineRepository.getDebitsForPeriod(acc.getId(), start, end);
                BigDecimal credits = journalLineRepository.getCreditsForPeriod(acc.getId(), start, end);
                BigDecimal net;
                if (acc.getType() == AccountType.EXPENSE) {
                    net = debits.subtract(credits);
                } else {
                    net = credits.subtract(debits);
                }

                if (net.compareTo(BigDecimal.ZERO) != 0) {
                    ReportDtos.ReportAccountItem item = ReportDtos.ReportAccountItem.builder()
                            .code(acc.getCode())
                            .name(acc.getName())
                            .balance(net)
                            .build();

                    if (acc.getType() == AccountType.INCOME) {
                        revenues.add(item);
                        totalRev = totalRev.add(net);
                    } else {
                        expenses.add(item);
                        totalExp = totalExp.add(net);
                    }
                }
            }
        }

        return ResponseEntity.ok(ReportDtos.ProfitAndLossReport.builder()
                .revenues(revenues)
                .totalRevenue(totalRev)
                .expenses(expenses)
                .totalExpenses(totalExp)
                .netIncome(totalRev.subtract(totalExp))
                .build());
    }

    @GetMapping("/reports/balance-sheet")
    @PreAuthorize("hasAuthority('ACCOUNTING_READ')")
    public ResponseEntity<ReportDtos.BalanceSheetReport> getBalanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long fiscalPeriodId) {

        LocalDate end = endDate;
        if (fiscalPeriodId != null) {
            FiscalPeriod fp = fiscalPeriodRepository.findById(fiscalPeriodId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fiscal period not found with ID: " + fiscalPeriodId));
            end = fp.getEndDate();
        }
        if (end == null) {
            end = LocalDate.now();
        }

        List<Account> accounts = accountRepository.findAll();
        List<ReportDtos.ReportAccountItem> assets = new ArrayList<>();
        List<ReportDtos.ReportAccountItem> liabilities = new ArrayList<>();
        List<ReportDtos.ReportAccountItem> equity = new ArrayList<>();

        BigDecimal totalAsset = BigDecimal.ZERO;
        BigDecimal totalLiab = BigDecimal.ZERO;
        BigDecimal totalEq = BigDecimal.ZERO;

        for (Account acc : accounts) {
            if (acc.getType() == AccountType.ASSET || acc.getType() == AccountType.LIABILITY || acc.getType() == AccountType.EQUITY) {
                BigDecimal bal = journalLineRepository.getBalanceUpToDate(acc.getId(), end);
                
                // Adjust sign for credit-normal accounts
                if (acc.getType() == AccountType.LIABILITY || acc.getType() == AccountType.EQUITY) {
                    bal = bal.negate();
                }

                if (bal.compareTo(BigDecimal.ZERO) != 0) {
                    ReportDtos.ReportAccountItem item = ReportDtos.ReportAccountItem.builder()
                            .code(acc.getCode())
                            .name(acc.getName())
                            .balance(bal)
                            .build();

                    if (acc.getType() == AccountType.ASSET) {
                        assets.add(item);
                        totalAsset = totalAsset.add(bal);
                    } else if (acc.getType() == AccountType.LIABILITY) {
                        liabilities.add(item);
                        totalLiab = totalLiab.add(bal);
                    } else {
                        equity.add(item);
                        totalEq = totalEq.add(bal);
                    }
                }
            }
        }

        return ResponseEntity.ok(ReportDtos.BalanceSheetReport.builder()
                .assets(assets)
                .totalAssets(totalAsset)
                .liabilities(liabilities)
                .totalLiabilities(totalLiab)
                .equity(equity)
                .totalEquity(totalEq)
                .totalLiabilitiesAndEquity(totalLiab.add(totalEq))
                .build());
    }

    @GetMapping("/reports/tax-summary")
    @PreAuthorize("hasAuthority('ACCOUNTING_READ')")
    public ResponseEntity<ReportDtos.TaxSummaryReport> getTaxSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long fiscalPeriodId) {

        LocalDate[] range = new LocalDate[2];
        resolveDateRange(startDate, endDate, fiscalPeriodId, range);
        LocalDate start = range[0];
        LocalDate end = range[1];

        // Find Tax Payable (Output Tax) (Code 2200) and Tax Receivable (Input Tax) (Code 1300)
        Account payableAcc = accountRepository.findByCode("2200").orElse(null);
        Account receivableAcc = accountRepository.findByCode("1300").orElse(null);

        List<ReportDtos.GeneralLedgerLine> collectedDetails = new ArrayList<>();
        List<ReportDtos.GeneralLedgerLine> paidDetails = new ArrayList<>();
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;

        if (payableAcc != null) {
            List<JournalLine> lines = journalLineRepository.findByAccountIdAndDateRange(payableAcc.getId(), start, end);
            for (JournalLine jl : lines) {
                // Tax collected is credited when selling
                BigDecimal amt = jl.getCreditAmount();
                if (amt.compareTo(BigDecimal.ZERO) > 0) {
                    totalCollected = totalCollected.add(amt);
                    collectedDetails.add(ReportDtos.GeneralLedgerLine.builder()
                            .entryDate(jl.getJournalEntry().getEntryDate())
                            .reference(jl.getJournalEntry().getReference())
                            .memo(jl.getDescription() != null ? jl.getDescription() : jl.getJournalEntry().getMemo())
                            .debitAmount(jl.getDebitAmount())
                            .creditAmount(jl.getCreditAmount())
                            .runningBalance(totalCollected)
                            .build());
                }
            }
        }

        if (receivableAcc != null) {
            List<JournalLine> lines = journalLineRepository.findByAccountIdAndDateRange(receivableAcc.getId(), start, end);
            for (JournalLine jl : lines) {
                // Tax paid is debited when purchasing
                BigDecimal amt = jl.getDebitAmount();
                if (amt.compareTo(BigDecimal.ZERO) > 0) {
                    totalPaid = totalPaid.add(amt);
                    paidDetails.add(ReportDtos.GeneralLedgerLine.builder()
                            .entryDate(jl.getJournalEntry().getEntryDate())
                            .reference(jl.getJournalEntry().getReference())
                            .memo(jl.getDescription() != null ? jl.getDescription() : jl.getJournalEntry().getMemo())
                            .debitAmount(jl.getDebitAmount())
                            .creditAmount(jl.getCreditAmount())
                            .runningBalance(totalPaid)
                            .build());
                }
            }
        }

        return ResponseEntity.ok(ReportDtos.TaxSummaryReport.builder()
                .taxCollected(totalCollected)
                .taxPaid(totalPaid)
                .netTaxPayable(totalCollected.subtract(totalPaid))
                .collectedDetails(collectedDetails)
                .paidDetails(paidDetails)
                .build());
    }
}
