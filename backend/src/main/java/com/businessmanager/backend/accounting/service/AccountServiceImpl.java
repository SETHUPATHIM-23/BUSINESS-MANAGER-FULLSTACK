package com.businessmanager.backend.accounting.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.enums.AccountType;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.repository.JournalLineRepository;
import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE", module = "ACCOUNTING")
    public Account createAccount(Account account) {
        if (accountRepository.existsByCode(account.getCode())) {
            throw new BusinessRuleException("Account code '" + account.getCode() + "' is already in use.");
        }
        return accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountByCode(String code) {
        return accountRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with code: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Account> searchAccounts(String search, AccountType type, Pageable pageable) {
        return accountRepository.searchAccounts(search, type, pageable);
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE", module = "ACCOUNTING")
    public Account updateAccount(Long id, Account accountDetails) {
        Account account = getAccountById(id);

        if (!account.getCode().equals(accountDetails.getCode())) {
            if (accountRepository.existsByCode(accountDetails.getCode())) {
                throw new BusinessRuleException("Account code '" + accountDetails.getCode() + "' is already in use.");
            }
            account.setCode(accountDetails.getCode());
        }

        account.setName(accountDetails.getName());
        account.setType(accountDetails.getType());
        account.setParent(accountDetails.getParent());
        account.setActive(accountDetails.isActive());

        return accountRepository.save(account);
    }

    @Override
    @Transactional
    @AuditAction(action = "DEACTIVATE", module = "ACCOUNTING")
    public void deactivateAccount(Long id) {
        Account account = getAccountById(id);
        account.setActive(false);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "ACCOUNTING")
    public void deleteAccount(Long id) {
        Account account = getAccountById(id);

        boolean hasHistory = !journalLineRepository.findByAccountId(id).isEmpty();
        if (hasHistory) {
            throw new BusinessRuleException("Account '" + account.getCode() + "' has existing transaction history and cannot be deleted. Deactivate instead.");
        }

        accountRepository.delete(account);
    }
}
