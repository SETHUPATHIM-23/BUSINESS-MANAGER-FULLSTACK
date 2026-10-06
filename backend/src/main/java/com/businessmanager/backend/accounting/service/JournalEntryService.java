package com.businessmanager.backend.accounting.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.FiscalPeriod;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface JournalEntryService {
    JournalEntry postJournalEntry(JournalEntry entry);
    JournalEntry getJournalEntryById(Long id);
    Page<JournalEntry> searchJournalEntries(String search, LocalDate startDate, LocalDate endDate, String sourceModule, Long fiscalPeriodId, Pageable pageable);
    
    Account resolveAccountByMappingKey(String mappingKey);

    // Fiscal Period management
    FiscalPeriod createFiscalPeriod(FiscalPeriod period);
    FiscalPeriod closeFiscalPeriod(Long periodId);
    FiscalPeriod reopenFiscalPeriod(Long periodId);
}
