package com.businessmanager.backend.accounting.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.AccountMapping;
import com.businessmanager.backend.accounting.entity.FiscalPeriod;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.entity.JournalLine;
import com.businessmanager.backend.accounting.enums.AccountType;
import com.businessmanager.backend.accounting.repository.AccountMappingRepository;
import com.businessmanager.backend.accounting.repository.FiscalPeriodRepository;
import com.businessmanager.backend.accounting.repository.JournalEntryRepository;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceImplTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Mock
    private AccountMappingRepository accountMappingRepository;

    @InjectMocks
    private JournalEntryServiceImpl journalEntryService;

    private Account debitAccount;
    private Account creditAccount;
    private FiscalPeriod openPeriod;
    private FiscalPeriod closedPeriod;
    private JournalEntry entry;

    @BeforeEach
    void setUp() {
        debitAccount = new Account();
        debitAccount.setId(10L);
        debitAccount.setCode("1010");
        debitAccount.setActive(true);

        creditAccount = new Account();
        creditAccount.setId(20L);
        creditAccount.setCode("4000");
        creditAccount.setActive(true);

        openPeriod = new FiscalPeriod();
        openPeriod.setId(1L);
        openPeriod.setName("FY2026-Q3");
        openPeriod.setStartDate(LocalDate.of(2026, 7, 1));
        openPeriod.setEndDate(LocalDate.of(2026, 9, 30));
        openPeriod.setClosed(false);

        closedPeriod = new FiscalPeriod();
        closedPeriod.setId(2L);
        closedPeriod.setName("FY2026-Q2");
        closedPeriod.setStartDate(LocalDate.of(2026, 4, 1));
        closedPeriod.setEndDate(LocalDate.of(2026, 6, 30));
        closedPeriod.setClosed(true);

        entry = new JournalEntry();
        entry.setEntryDate(LocalDate.of(2026, 7, 30));
        entry.setReference("JE-001");
        entry.setMemo("Test manual entry");
        entry.setSourceModule("MANUAL");

        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(debitAccount);
        debitLine.setDebitAmount(new BigDecimal("100.00"));
        debitLine.setCreditAmount(BigDecimal.ZERO);
        entry.addLine(debitLine);

        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(creditAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(new BigDecimal("100.00"));
        entry.addLine(creditLine);
    }

    @Test
    void postJournalEntry_Success() {
        when(fiscalPeriodRepository.findActivePeriodForDate(entry.getEntryDate())).thenReturn(Optional.of(openPeriod));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> i.getArgument(0));

        JournalEntry result = journalEntryService.postJournalEntry(entry);

        assertNotNull(result);
        assertEquals(openPeriod, result.getFiscalPeriod());
        verify(journalEntryRepository).save(entry);
    }

    @Test
    void postJournalEntry_Unbalanced_ThrowsException() {
        // Adjust credit line to be unbalanced (90.00 credit vs 100.00 debit)
        entry.getLines().get(1).setCreditAmount(new BigDecimal("90.00"));

        assertThrows(BusinessRuleException.class, () -> journalEntryService.postJournalEntry(entry));
        verify(journalEntryRepository, never()).save(any(JournalEntry.class));
    }

    @Test
    void postJournalEntry_ZeroTotalAmount_ThrowsException() {
        // Both sides are zero
        entry.getLines().get(0).setDebitAmount(BigDecimal.ZERO);
        entry.getLines().get(1).setCreditAmount(BigDecimal.ZERO);

        assertThrows(BusinessRuleException.class, () -> journalEntryService.postJournalEntry(entry));
    }

    @Test
    void postJournalEntry_NegativeAmount_ThrowsException() {
        entry.getLines().get(0).setDebitAmount(new BigDecimal("-10.00"));

        assertThrows(BusinessRuleException.class, () -> journalEntryService.postJournalEntry(entry));
    }

    @Test
    void postJournalEntry_InactiveAccount_ThrowsException() {
        debitAccount.setActive(false);

        assertThrows(BusinessRuleException.class, () -> journalEntryService.postJournalEntry(entry));
    }

    @Test
    void postJournalEntry_NoFiscalPeriod_ThrowsException() {
        when(fiscalPeriodRepository.findActivePeriodForDate(entry.getEntryDate())).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> journalEntryService.postJournalEntry(entry));
    }

    @Test
    void postJournalEntry_ClosedFiscalPeriod_ThrowsException() {
        entry.setEntryDate(LocalDate.of(2026, 5, 15)); // Falls inside closedPeriod
        when(fiscalPeriodRepository.findActivePeriodForDate(entry.getEntryDate())).thenReturn(Optional.of(closedPeriod));

        assertThrows(BusinessRuleException.class, () -> journalEntryService.postJournalEntry(entry));
    }

    @Test
    void resolveAccountByMappingKey_Success() {
        AccountMapping mapping = new AccountMapping();
        mapping.setMappingKey("CASH");
        mapping.setAccount(debitAccount);

        when(accountMappingRepository.findByMappingKey("CASH")).thenReturn(Optional.of(mapping));

        Account result = journalEntryService.resolveAccountByMappingKey("CASH");

        assertNotNull(result);
        assertEquals("1010", result.getCode());
    }

    @Test
    void resolveAccountByMappingKey_NotFound_ThrowsException() {
        when(accountMappingRepository.findByMappingKey("MISSING")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> journalEntryService.resolveAccountByMappingKey("MISSING"));
    }

    @Test
    void closeFiscalPeriod_Success() {
        when(fiscalPeriodRepository.findById(1L)).thenReturn(Optional.of(openPeriod));
        when(fiscalPeriodRepository.save(any(FiscalPeriod.class))).thenAnswer(i -> i.getArgument(0));

        FiscalPeriod result = journalEntryService.closeFiscalPeriod(1L);

        assertTrue(result.isClosed());
    }

    @Test
    void reopenFiscalPeriod_Success() {
        when(fiscalPeriodRepository.findById(2L)).thenReturn(Optional.of(closedPeriod));
        when(fiscalPeriodRepository.save(any(FiscalPeriod.class))).thenAnswer(i -> i.getArgument(0));

        FiscalPeriod result = journalEntryService.reopenFiscalPeriod(2L);

        assertFalse(result.isClosed());
    }
}
