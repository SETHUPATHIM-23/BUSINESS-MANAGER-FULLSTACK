package com.businessmanager.backend.fund.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.DuplicateResourceException;
import com.businessmanager.backend.fund.entity.FundAccount;
import com.businessmanager.backend.fund.entity.FundAccountType;
import com.businessmanager.backend.fund.repository.FundAccountRepository;
import com.businessmanager.backend.fund.repository.FundTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundAccountServiceImplTest {

    @Mock
    private FundAccountRepository fundAccountRepository;

    @Mock
    private FundTransactionRepository fundTransactionRepository;

    @InjectMocks
    private FundAccountServiceImpl fundAccountService;

    private FundAccount bankAccount;
    private FundAccount cashAccount;

    @BeforeEach
    void setUp() {
        bankAccount = new FundAccount();
        bankAccount.setId(1L);
        bankAccount.setName("Main Checking");
        bankAccount.setType(FundAccountType.BANK);
        bankAccount.setAccountNumber("123456789");
        bankAccount.setActive(true);

        cashAccount = new FundAccount();
        cashAccount.setId(2L);
        cashAccount.setName("Petty Cash");
        cashAccount.setType(FundAccountType.CASH);
        cashAccount.setActive(true);
    }

    @Test
    void createFundAccount_Success_Cash() {
        when(fundAccountRepository.save(any(FundAccount.class))).thenReturn(cashAccount);
        FundAccount created = fundAccountService.createFundAccount(cashAccount);
        assertNotNull(created);
        assertEquals(FundAccountType.CASH, created.getType());
        verify(fundAccountRepository).save(cashAccount);
    }

    @Test
    void createFundAccount_Success_Bank() {
        when(fundAccountRepository.existsByAccountNumber("123456789")).thenReturn(false);
        when(fundAccountRepository.save(any(FundAccount.class))).thenReturn(bankAccount);

        FundAccount created = fundAccountService.createFundAccount(bankAccount);

        assertNotNull(created);
        assertEquals("123456789", created.getAccountNumber());
        verify(fundAccountRepository).save(bankAccount);
    }

    @Test
    void createFundAccount_ThrowsBusinessRuleException_WhenBankHasNoAccountNumber() {
        bankAccount.setAccountNumber(null);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> fundAccountService.createFundAccount(bankAccount));
        assertTrue(ex.getMessage().contains("must have an account number"));
    }

    @Test
    void createFundAccount_ThrowsDuplicateResourceException_WhenAccountNumberExists() {
        when(fundAccountRepository.existsByAccountNumber("123456789")).thenReturn(true);
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> fundAccountService.createFundAccount(bankAccount));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void updateFundAccount_Success() {
        when(fundAccountRepository.findById(1L)).thenReturn(Optional.of(bankAccount));
        when(fundAccountRepository.existsByAccountNumberAndIdNot("987654321", 1L)).thenReturn(false);
        when(fundAccountRepository.save(any(FundAccount.class))).thenReturn(bankAccount);

        FundAccount details = new FundAccount();
        details.setName("Updated Bank");
        details.setType(FundAccountType.BANK);
        details.setAccountNumber("987654321");
        details.setActive(true);

        FundAccount updated = fundAccountService.updateFundAccount(1L, details);
        assertNotNull(updated);
        verify(fundAccountRepository).save(bankAccount);
        assertEquals("Updated Bank", bankAccount.getName());
    }

    @Test
    void deactivateFundAccount_Success() {
        when(fundAccountRepository.findById(1L)).thenReturn(Optional.of(bankAccount));
        when(fundAccountRepository.save(any(FundAccount.class))).thenReturn(bankAccount);

        FundAccount deactivated = fundAccountService.deactivateFundAccount(1L);
        assertFalse(deactivated.getActive());
    }

    @Test
    void deleteFundAccount_Success_WhenNoTransactions() {
        when(fundAccountRepository.findById(2L)).thenReturn(Optional.of(cashAccount));
        when(fundTransactionRepository.searchTransactions(eq(2L), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        assertDoesNotThrow(() -> fundAccountService.deleteFundAccount(2L));
        verify(fundAccountRepository).delete(cashAccount);
    }

    @Test
    void deleteFundAccount_ThrowsBusinessRuleException_WhenTransactionsExist() {
        when(fundAccountRepository.findById(1L)).thenReturn(Optional.of(bankAccount));
        when(fundTransactionRepository.searchTransactions(eq(1L), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new com.businessmanager.backend.fund.entity.FundTransaction()), PageRequest.of(0, 1), 1));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> fundAccountService.deleteFundAccount(1L));
        assertTrue(ex.getMessage().contains("cannot be deleted. Deactivate it instead"));
        verify(fundAccountRepository, never()).delete(any());
    }
}
