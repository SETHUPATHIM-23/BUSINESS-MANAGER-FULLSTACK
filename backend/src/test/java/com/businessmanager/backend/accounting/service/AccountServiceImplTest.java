package com.businessmanager.backend.accounting.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.JournalLine;
import com.businessmanager.backend.accounting.enums.AccountType;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.repository.JournalLineRepository;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JournalLineRepository journalLineRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setCode("1010");
        account.setName("Cash on Hand");
        account.setType(AccountType.ASSET);
        account.setActive(true);
    }

    @Test
    void createAccount_Success() {
        when(accountRepository.existsByCode("1010")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        Account result = accountService.createAccount(account);

        assertNotNull(result);
        assertEquals("1010", result.getCode());
        verify(accountRepository).save(account);
    }

    @Test
    void createAccount_DuplicateCode_ThrowsException() {
        when(accountRepository.existsByCode("1010")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> accountService.createAccount(account));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAccountById_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        Account result = accountService.getAccountById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getAccountById_NotFound_ThrowsException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountById(99L));
    }

    @Test
    void updateAccount_Success() {
        Account details = new Account();
        details.setCode("1010");
        details.setName("Cash Main");
        details.setType(AccountType.ASSET);
        details.setActive(true);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account result = accountService.updateAccount(1L, details);

        assertEquals("Cash Main", result.getName());
        verify(accountRepository).save(account);
    }

    @Test
    void updateAccount_ChangeCodeSuccess() {
        Account details = new Account();
        details.setCode("1011");
        details.setName("Cash Main");
        details.setType(AccountType.ASSET);
        details.setActive(true);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.existsByCode("1011")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account result = accountService.updateAccount(1L, details);

        assertEquals("1011", result.getCode());
    }

    @Test
    void updateAccount_ChangeCodeDuplicate_ThrowsException() {
        Account details = new Account();
        details.setCode("1020");
        details.setName("Cash Main");
        details.setType(AccountType.ASSET);
        details.setActive(true);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.existsByCode("1020")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> accountService.updateAccount(1L, details));
    }

    @Test
    void deactivateAccount_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.deactivateAccount(1L);

        assertFalse(account.isActive());
        verify(accountRepository).save(account);
    }

    @Test
    void deleteAccount_NoHistory_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(journalLineRepository.findByAccountId(1L)).thenReturn(Collections.emptyList());

        accountService.deleteAccount(1L);

        verify(accountRepository).delete(account);
    }

    @Test
    void deleteAccount_WithHistory_ThrowsException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(journalLineRepository.findByAccountId(1L)).thenReturn(Collections.singletonList(new JournalLine()));

        assertThrows(BusinessRuleException.class, () -> accountService.deleteAccount(1L));
        verify(accountRepository, never()).delete(any(Account.class));
    }
}
