package com.businessmanager.backend.fund.controller;

import com.businessmanager.backend.fund.dto.FundAccountCreateRequest;
import com.businessmanager.backend.fund.dto.FundTransferRequest;
import com.businessmanager.backend.fund.entity.FundAccount;
import com.businessmanager.backend.fund.entity.FundAccountType;
import com.businessmanager.backend.fund.entity.FundTransaction;
import com.businessmanager.backend.fund.entity.FundTransactionType;
import com.businessmanager.backend.fund.service.FundAccountService;
import com.businessmanager.backend.fund.service.FundTransactionService;
import com.businessmanager.backend.fund.service.ReconciliationRecordService;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class FundControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private FundAccountService fundAccountService;
    @MockBean private FundTransactionService fundTransactionService;
    @MockBean private ReconciliationRecordService reconciliationRecordService;
    
    // Auth mocks
    @MockBean private JwtTokenProvider tokenProvider;
    @MockBean private CustomUserDetailsService userDetailsService;

    private FundAccount cashAccount;
    private FundAccount bankAccount;

    @BeforeEach
    void setUp() {
        cashAccount = new FundAccount();
        cashAccount.setId(1L);
        cashAccount.setName("Petty Cash");
        cashAccount.setType(FundAccountType.CASH);
        
        bankAccount = new FundAccount();
        bankAccount.setId(2L);
        bankAccount.setName("Main Bank");
        bankAccount.setType(FundAccountType.BANK);
        bankAccount.setAccountNumber("12345");
    }

    // ── AUTHORIZATION GATING TESTS ─────────────────────────────────────

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void getAccounts_NoPermission_Forbidden() throws Exception {
        mockMvc.perform(get("/api/funds/accounts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FUND_READ")
    public void getAccounts_HasPermission_Ok() throws Exception {
        when(fundAccountService.searchFundAccounts(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(cashAccount)));

        mockMvc.perform(get("/api/funds/accounts"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "FUND_READ")
    public void createAccount_ReadOnlyUser_Forbidden() throws Exception {
        FundAccountCreateRequest req = FundAccountCreateRequest.builder()
                .name("New Account")
                .type(FundAccountType.CASH)
                .build();

        mockMvc.perform(post("/api/funds/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FUND_WRITE")
    public void createAccount_WriteUser_Ok() throws Exception {
        FundAccountCreateRequest req = FundAccountCreateRequest.builder()
                .name("New Account")
                .type(FundAccountType.CASH)
                .build();

        when(fundAccountService.createFundAccount(any(FundAccount.class))).thenReturn(cashAccount);

        mockMvc.perform(post("/api/funds/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // ── VALIDATION ERROR TESTS ──────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "FUND_WRITE")
    public void createAccount_InvalidRequest_BadRequest() throws Exception {
        FundAccountCreateRequest req = FundAccountCreateRequest.builder()
                .name("") // Blank name
                .type(null) // Null type
                .build();

        mockMvc.perform(post("/api/funds/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @WithMockUser(authorities = "FUND_WRITE")
    public void createAccount_BankWithoutAccountNumber_BadRequest() throws Exception {
        FundAccountCreateRequest req = FundAccountCreateRequest.builder()
                .name("Bank Account")
                .type(FundAccountType.BANK)
                .accountNumber("") // Blank account number for Bank
                .build();

        mockMvc.perform(post("/api/funds/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @WithMockUser(authorities = "FUND_WRITE")
    public void transferFunds_NegativeAmount_BadRequest() throws Exception {
        FundTransferRequest req = FundTransferRequest.builder()
                .sourceAccountId(1L)
                .targetAccountId(2L)
                .amount(new BigDecimal("-50.00")) // Negative amount
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/funds/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // ── END-TO-END INTER-FUND TRANSFER FLOW ─────────────────────────────

    @Test
    @WithMockUser(authorities = "FUND_WRITE")
    public void transferFunds_EndToEnd_Ok() throws Exception {
        FundTransferRequest req = FundTransferRequest.builder()
                .sourceAccountId(2L)
                .targetAccountId(1L)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Replenish Petty Cash")
                .build();

        FundTransaction mockTx = new FundTransaction();
        mockTx.setId(10L);
        mockTx.setFundAccount(bankAccount); // Source
        mockTx.setTargetFundAccount(cashAccount); // Target
        mockTx.setType(FundTransactionType.TRANSFER);
        mockTx.setAmount(new BigDecimal("500.00"));

        when(fundTransactionService.transferFunds(
                eq(2L), eq(1L), eq(new BigDecimal("500.00")), any(LocalDate.class), eq("Replenish Petty Cash")))
                .thenReturn(mockTx);

        mockMvc.perform(post("/api/funds/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fundAccountId").value(2))
                .andExpect(jsonPath("$.targetFundAccountId").value(1))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.type").value("TRANSFER"));

        verify(fundTransactionService, times(1)).transferFunds(any(), any(), any(), any(), any());
    }
}
