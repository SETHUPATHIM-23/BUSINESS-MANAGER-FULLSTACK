package com.businessmanager.backend.accounting.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.AccountMapping;
import com.businessmanager.backend.accounting.entity.FiscalPeriod;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.entity.JournalLine;
import com.businessmanager.backend.accounting.repository.AccountMappingRepository;
import com.businessmanager.backend.accounting.repository.FiscalPeriodRepository;
import com.businessmanager.backend.accounting.repository.JournalEntryRepository;
import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.repository.JournalLineRepository;
import com.businessmanager.backend.accounting.enums.AccountType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class JournalEntryServiceImpl implements JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final AccountMappingRepository accountMappingRepository;
    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;

    @Override
    @Transactional
    @AuditAction(action = "POST", module = "ACCOUNTING")
    public JournalEntry postJournalEntry(JournalEntry entry) {
        if (entry == null) {
            throw new BusinessRuleException("Journal entry cannot be null.");
        }
        if (entry.getLines() == null || entry.getLines().isEmpty()) {
            throw new BusinessRuleException("Journal entry must contain at least one line.");
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalLine line : entry.getLines()) {
            if (line.getAccount() == null) {
                throw new BusinessRuleException("Each journal line must specify a valid account.");
            }
            if (!line.getAccount().isActive()) {
                throw new BusinessRuleException("Cannot post to deactivated account: " + line.getAccount().getCode());
            }
            if (line.getDebitAmount() == null) line.setDebitAmount(BigDecimal.ZERO);
            if (line.getCreditAmount() == null) line.setCreditAmount(BigDecimal.ZERO);

            if (line.getDebitAmount().compareTo(BigDecimal.ZERO) < 0 || line.getCreditAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("Debit and Credit amounts must be non-negative.");
            }

            totalDebits = totalDebits.add(line.getDebitAmount());
            totalCredits = totalCredits.add(line.getCreditAmount());

            // Bind line to entry
            line.setJournalEntry(entry);
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessRuleException("Journal entry is unbalanced! Debits: " + totalDebits + ", Credits: " + totalCredits);
        }
        if (totalDebits.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("Journal entry total debit/credit must be greater than zero.");
        }

        // Resolve active fiscal period
        FiscalPeriod period = fiscalPeriodRepository.findActivePeriodForDate(entry.getEntryDate())
                .orElseThrow(() -> new BusinessRuleException("No fiscal period defined for entry date: " + entry.getEntryDate()));

        if (period.isClosed()) {
            throw new BusinessRuleException("Fiscal period '" + period.getName() + "' is closed. Posting denied.");
        }

        entry.setFiscalPeriod(period);

        return journalEntryRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public JournalEntry getJournalEntryById(Long id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JournalEntry> searchJournalEntries(String search, LocalDate startDate, LocalDate endDate, String sourceModule, Long fiscalPeriodId, Pageable pageable) {
        return journalEntryRepository.searchJournalEntries(search, startDate, endDate, sourceModule, fiscalPeriodId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Account resolveAccountByMappingKey(String mappingKey) {
        return accountMappingRepository.findByMappingKey(mappingKey)
                .map(AccountMapping::getAccount)
                .orElseGet(() -> {
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

                    Account fallback = accountRepository.findByCode(defaultCode)
                            .or(() -> accountRepository.findAll().stream().findFirst())
                            .orElseThrow(() -> new BusinessRuleException("Account mapping not configured for key: " + mappingKey));

                    try {
                        AccountMapping newMapping = new AccountMapping();
                        newMapping.setMappingKey(mappingKey);
                        newMapping.setAccount(fallback);
                        newMapping.setCreatedBy("system");
                        newMapping.setUpdatedBy("system");
                        accountMappingRepository.save(newMapping);
                    } catch (Exception ignored) {}

                    return fallback;
                });
    }

    @Override
    @Transactional
    @AuditAction(action = "CREATE_PERIOD", module = "ACCOUNTING")
    public FiscalPeriod createFiscalPeriod(FiscalPeriod period) {
        if (fiscalPeriodRepository.findByName(period.getName()).isPresent()) {
            throw new BusinessRuleException("Fiscal period name '" + period.getName() + "' is already in use.");
        }
        return fiscalPeriodRepository.save(period);
    }

    @Override
    @Transactional
    @AuditAction(action = "CLOSE_PERIOD", module = "ACCOUNTING")
    public FiscalPeriod closeFiscalPeriod(Long periodId) {
        FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiscal period not found with ID: " + periodId));
        
        if (period.isClosed()) {
            throw new BusinessRuleException("Period is already closed.");
        }

        // Generate Fiscal Year Close Sweeping Entry
        generateClosingSweepEntry(period);

        period.setClosed(true);
        return fiscalPeriodRepository.save(period);
    }

    private void generateClosingSweepEntry(FiscalPeriod period) {
        Account equityAccount = resolveAccountByMappingKey("EQUITY");
        
        java.util.List<Account> plAccounts = accountRepository.findAll().stream()
                .filter(a -> a.getType() == AccountType.REVENUE || 
                             a.getType() == AccountType.COST_OF_GOODS_SOLD || 
                             a.getType() == AccountType.EXPENSE)
                .collect(java.util.stream.Collectors.toList());

        JournalEntry closingEntry = new JournalEntry();
        closingEntry.setEntryDate(period.getEndDate());
        closingEntry.setReferenceType("FISCAL_CLOSE");
        closingEntry.setReferenceId(period.getId());
        closingEntry.setReferenceNumber("CLOSE-" + period.getName());
        closingEntry.setDescription("Fiscal Year Close Sweep for period: " + period.getName());

        BigDecimal totalNetIncome = BigDecimal.ZERO;

        for (Account acc : plAccounts) {
            BigDecimal netBalance = journalLineRepository.getBalanceUpToDate(acc.getId(), period.getEndDate());
            if (netBalance.compareTo(BigDecimal.ZERO) == 0) continue;

            JournalLine line = new JournalLine();
            line.setAccount(acc);
            line.setDescription("Closing Entry — " + acc.getName());

            if (netBalance.compareTo(BigDecimal.ZERO) > 0) {
                // Debit balance (Expense/COGS), so we credit to zero it out
                line.setDebitAmount(BigDecimal.ZERO);
                line.setCreditAmount(netBalance);
                totalNetIncome = totalNetIncome.subtract(netBalance);
            } else {
                // Credit balance (Revenue), so we debit to zero it out
                line.setDebitAmount(netBalance.negate());
                line.setCreditAmount(BigDecimal.ZERO);
                totalNetIncome = totalNetIncome.add(netBalance.negate());
            }
            closingEntry.addLine(line);
        }

        if (closingEntry.getLines().isEmpty()) {
            return; // Nothing to close
        }

        // Balance against Equity (Retained Earnings)
        JournalLine equityLine = new JournalLine();
        equityLine.setAccount(equityAccount);
        equityLine.setDescription("Retained Earnings — " + period.getName());
        if (totalNetIncome.compareTo(BigDecimal.ZERO) > 0) {
            // Net profit, credit equity
            equityLine.setDebitAmount(BigDecimal.ZERO);
            equityLine.setCreditAmount(totalNetIncome);
        } else if (totalNetIncome.compareTo(BigDecimal.ZERO) < 0) {
            // Net loss, debit equity
            equityLine.setDebitAmount(totalNetIncome.negate());
            equityLine.setCreditAmount(BigDecimal.ZERO);
        } else {
            equityLine.setDebitAmount(BigDecimal.ZERO);
            equityLine.setCreditAmount(BigDecimal.ZERO);
        }
        
        if (equityLine.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 || equityLine.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
             closingEntry.addLine(equityLine);
        }

        // Post bypassing normal period checks because this is the closing entry
        closingEntry.setFiscalPeriod(period);
        
        // Ensure it's balanced
        BigDecimal totalDebits = closingEntry.getLines().stream().map(JournalLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = closingEntry.getLines().stream().map(JournalLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessRuleException("Failed to generate balanced closing entry. DR: " + totalDebits + ", CR: " + totalCredits);
        }

        // Link lines
        closingEntry.getLines().forEach(l -> l.setJournalEntry(closingEntry));
        
        journalEntryRepository.save(closingEntry);
    }

    @Override
    @Transactional
    @AuditAction(action = "REOPEN_PERIOD", module = "ACCOUNTING")
    public FiscalPeriod reopenFiscalPeriod(Long periodId) {
        FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiscal period not found with ID: " + periodId));
        period.setClosed(false);
        return fiscalPeriodRepository.save(period);
    }
}
