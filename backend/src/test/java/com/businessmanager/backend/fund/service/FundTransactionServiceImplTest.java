package com.businessmanager.backend.fund.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.fund.entity.FundAccount;
import com.businessmanager.backend.fund.entity.FundAccountType;
import com.businessmanager.backend.fund.entity.FundTransaction;
import com.businessmanager.backend.fund.entity.FundTransactionType;
import com.businessmanager.backend.fund.repository.FundAccountRepository;
import com.businessmanager.backend.fund.repository.FundTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class FundTransactionServiceImplTest {

    @Mock
    private FundTransactionRepository fundTransactionRepository;

    @Mock
    private FundAccountRepository fundAccountRepository;

    @InjectMocks
    private FundTransactionServiceImpl fundTransactionService;

    private FundAccount bankAccount;
    private FundAccount cashAccount;
    private FundTransaction existingTransaction;

    @BeforeEach
    void setUp() {
        bankAccount = new FundAccount();
        bankAccount.setId(1L);
        bankAccount.setName("Main Checking");
        bankAccount.setType(FundAccountType.BANK);
        bankAccount.setCurrentBalance(new BigDecimal("1000.00"));
        bankAccount.setActive(true);

        cashAccount = new FundAccount();
        cashAccount.setId(2L);
        cashAccount.setName("Petty Cash");
        cashAccount.setType(FundAccountType.CASH);
        cashAccount.setCurrentBalance(new BigDecimal("100.00"));
        cashAccount.setActive(true);

        existingTransaction = new FundTransaction();
        existingTransaction.setId(10L);
        existingTransaction.setFundAccount(bankAccount);
        existingTransaction.setType(FundTransactionType.RECEIPT);
        existingTransaction.setAmount(new BigDecimal("500.00"));
    }

    @Test
    void recordCustomerPayment_Success() {
        when(fundAccountRepository.findById(1L)).thenReturn(Optional.of(bankAccount));
        when(fundTransactionRepository.save(any(FundTransaction.class))).thenAnswer(i -> i.getArguments()[0]);

        FundTransaction tx = fundTransactionService.recordCustomerPayment(1L, 101L, new BigDecimal("200.00"), LocalDate.now());

        assertNotNull(tx);
        assertEquals(FundTransactionType.RECEIPT, tx.getType());
        assertEquals(new BigDecimal("1200.00"), bankAccount.getCurrentBalance()); // 1000 + 200
        verify(fundAccountRepository).save(bankAccount);
        verify(fundTransactionRepository).save(tx);
    }

    @Test
    void recordSupplierPayment_Success_WithSufficientFunds() {
        when(fundAccountRepository.findById(2L)).thenReturn(Optional.of(cashAccount));
        when(fundTransactionRepository.save(any(FundTransaction.class))).thenAnswer(i -> i.getArguments()[0]);

        FundTransaction tx = fundTransactionService.recordSupplierPayment(2L, 202L, new BigDecimal("50.00"), LocalDate.now());

        assertNotNull(tx);
        assertEquals(FundTransactionType.PAYMENT, tx.getType());
        assertEquals(new BigDecimal("50.00"), cashAccount.getCurrentBalance()); // 100 - 50
    }

    @Test
    void recordSupplierPayment_ThrowsException_WhenCashAccountHasInsufficientFunds() {
        when(fundAccountRepository.findById(2L)).thenReturn(Optional.of(cashAccount));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                fundTransactionService.recordSupplierPayment(2L, 202L, new BigDecimal("150.00"), LocalDate.now())
        );
        assertTrue(ex.getMessage().contains("Insufficient funds"));
        verify(fundTransactionRepository, never()).save(any());
    }

    @Test
    void transferFunds_Success() {
        when(fundAccountRepository.findById(1L)).thenReturn(Optional.of(bankAccount));
        when(fundAccountRepository.findById(2L)).thenReturn(Optional.of(cashAccount));
        when(fundTransactionRepository.save(any(FundTransaction.class))).thenAnswer(i -> i.getArguments()[0]);

        FundTransaction tx = fundTransactionService.transferFunds(1L, 2L, new BigDecimal("300.00"), LocalDate.now(), "ATM Withdrawal");

        assertNotNull(tx);
        assertEquals(FundTransactionType.TRANSFER, tx.getType());
        assertEquals(new BigDecimal("700.00"), bankAccount.getCurrentBalance()); // 1000 - 300
        assertEquals(new BigDecimal("400.00"), cashAccount.getCurrentBalance()); // 100 + 300

        ArgumentCaptor<FundTransaction> captor = ArgumentCaptor.forClass(FundTransaction.class);
        verify(fundTransactionRepository).save(captor.capture());
        FundTransaction savedTx = captor.getValue();
        assertEquals(1L, savedTx.getFundAccount().getId());
        assertEquals(2L, savedTx.getTargetFundAccount().getId());
    }

    @Test
    void transferFunds_ThrowsException_WhenSourceAccountHasInsufficientFunds() {
        when(fundAccountRepository.findById(2L)).thenReturn(Optional.of(cashAccount)); // Source
        when(fundAccountRepository.findById(1L)).thenReturn(Optional.of(bankAccount)); // Target

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                fundTransactionService.transferFunds(2L, 1L, new BigDecimal("200.00"), LocalDate.now(), "Deposit")
        );
        assertTrue(ex.getMessage().contains("Insufficient funds"));
    }

    @Test
    void reverseTransaction_Success() {
        when(fundTransactionRepository.findById(10L)).thenReturn(Optional.of(existingTransaction));
        when(fundAccountRepository.findById(1L)).thenReturn(Optional.of(bankAccount));
        when(fundTransactionRepository.save(any(FundTransaction.class))).thenAnswer(i -> i.getArguments()[0]);

        FundTransaction reversedTx = fundTransactionService.reverseTransaction(10L, "Mistake");

        assertNotNull(reversedTx);
        assertEquals(FundTransactionType.PAYMENT, reversedTx.getType()); // Reversing a receipt -> payment
        assertEquals(new BigDecimal("500.00"), reversedTx.getAmount());
        assertEquals(10L, reversedTx.getReferenceDocumentId());
        assertEquals("REVERSAL", reversedTx.getReferenceDocumentType());
        assertTrue(reversedTx.getDescription().contains("Mistake"));
        assertEquals(new BigDecimal("500.00"), bankAccount.getCurrentBalance()); // 1000 - 500
    }

    @Test
    void deleteTransaction_ThrowsBusinessRuleException() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                fundTransactionService.deleteTransaction(10L)
        );
        assertTrue(ex.getMessage().contains("immutable and cannot be deleted"));
    }
}
