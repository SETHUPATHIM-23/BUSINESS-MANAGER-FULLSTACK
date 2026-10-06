package com.businessmanager.backend.accounting.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.enums.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {
    Account createAccount(Account account);
    Account getAccountById(Long id);
    Account getAccountByCode(String code);
    Page<Account> searchAccounts(String search, AccountType type, Pageable pageable);
    Account updateAccount(Long id, Account accountDetails);
    void deactivateAccount(Long id);
    void deleteAccount(Long id);
}
