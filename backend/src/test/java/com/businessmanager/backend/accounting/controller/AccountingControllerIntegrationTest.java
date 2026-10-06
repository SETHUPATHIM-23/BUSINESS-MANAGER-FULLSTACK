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
import com.businessmanager.backend.accounting.service.AccountService;
import com.businessmanager.backend.accounting.service.JournalEntryService;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AccountingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private AccountService accountService;
    @MockBean private JournalEntryService journalEntryService;
    @MockBean private AccountRepository accountRepository;
    @MockBean private FiscalPeriodRepository fiscalPeriodRepository;
    @MockBean private JwtTokenProvider tokenProvider;
    @MockBean private CustomUserDetailsService userDetailsService;

    private Account account;
    private FiscalPeriod fiscalPeriod;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setCode("1010");
        account.setName("Cash");
        account.setType(AccountType.ASSET);
        account.setActive(true);

        fiscalPeriod = new FiscalPeriod();
        fiscalPeriod.setId(100L);
        fiscalPeriod.setName("FY2026-Q3");
        fiscalPeriod.setStartDate(LocalDate.of(2026, 7, 1));
        fiscalPeriod.setEndDate(LocalDate.of(2026, 9, 30));
        fiscalPeriod.setClosed(false);
    }

    // ── Authorization Gating Tests ─────────────────────────────────────

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void getAccounts_NoPermission_Forbidden() throws Exception {
        mockMvc.perform(get("/api/accountings/accounts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ACCOUNTING_READ")
    public void getAccounts_HasPermission_Ok() throws Exception {
        mockMvc.perform(get("/api/accountings/accounts"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ACCOUNTING_READ")
    public void createAccount_ReadOnlyUser_Forbidden() throws Exception {
        AccountCreateRequest req = new AccountCreateRequest();
        req.setCode("1020");
        req.setName("Bank");
        req.setType(AccountType.ASSET);

        mockMvc.perform(post("/api/accountings/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ACCOUNTING_WRITE")
    public void createFiscalPeriod_AccountantRole_Forbidden() throws Exception {
        FiscalPeriodRequest req = new FiscalPeriodRequest();
        req.setName("FY2026-Q4");
        req.setStartDate(LocalDate.of(2026, 10, 1));
        req.setEndDate(LocalDate.of(2026, 12, 31));

        mockMvc.perform(post("/api/accountings/periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    public void createFiscalPeriod_AdminRole_Ok() throws Exception {
        FiscalPeriodRequest req = new FiscalPeriodRequest();
        req.setName("FY2026-Q4");
        req.setStartDate(LocalDate.of(2026, 10, 1));
        req.setEndDate(LocalDate.of(2026, 12, 31));

        when(journalEntryService.createFiscalPeriod(any(FiscalPeriod.class))).thenReturn(fiscalPeriod);

        mockMvc.perform(post("/api/accountings/periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // ── Validation Errors Tests ─────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "ACCOUNTING_WRITE")
    public void createAccount_InvalidRequest_BadRequest() throws Exception {
        AccountCreateRequest req = new AccountCreateRequest();
        req.setCode(""); // Blank code
        req.setName("Cash");
        req.setType(null); // Missing type

        mockMvc.perform(post("/api/accountings/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @WithMockUser(authorities = "ACCOUNTING_WRITE")
    public void postJournalEntry_InvalidLines_BadRequest() throws Exception {
        JournalEntryCreateRequest req = new JournalEntryCreateRequest();
        req.setEntryDate(LocalDate.now());
        req.setReference("JE-001");
        req.setLines(Collections.emptyList()); // Empty lines

        mockMvc.perform(post("/api/accountings/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── End-To-End Business Flow & Failure Path Tests ───────────────────

    @Test
    @WithMockUser(authorities = "ACCOUNTING_WRITE")
    public void postJournalEntry_ClosedPeriod_Conflict() throws Exception {
        JournalEntryCreateRequest req = new JournalEntryCreateRequest();
        req.setEntryDate(LocalDate.of(2026, 5, 1));
        req.setReference("JE-002");
        req.setMemo("Closing entry");
        req.setSourceModule("MANUAL");

        JournalLineRequest dr = new JournalLineRequest();
        dr.setAccountId(1L);
        dr.setDebitAmount(new BigDecimal("100.00"));
        dr.setCreditAmount(BigDecimal.ZERO);

        JournalLineRequest cr = new JournalLineRequest();
        cr.setAccountId(2L);
        cr.setDebitAmount(BigDecimal.ZERO);
        cr.setCreditAmount(new BigDecimal("100.00"));

        req.setLines(List.of(dr, cr));

        // Stub services
        when(accountService.getAccountById(1L)).thenReturn(account);
        when(accountService.getAccountById(2L)).thenReturn(account);
        when(journalEntryService.postJournalEntry(any(JournalEntry.class)))
                .thenThrow(new BusinessRuleException("Fiscal period is closed. Posting denied."));

        mockMvc.perform(post("/api/accountings/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Fiscal period is closed. Posting denied."));
    }
}
