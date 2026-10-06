package com.businessmanager.backend.fund.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.DuplicateResourceException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.fund.entity.FundAccount;
import com.businessmanager.backend.fund.entity.FundAccountType;
import com.businessmanager.backend.fund.repository.FundAccountRepository;
import com.businessmanager.backend.fund.repository.FundTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FundAccountServiceImpl implements FundAccountService {

    private final FundAccountRepository fundAccountRepository;
    private final FundTransactionRepository fundTransactionRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE", module = "FUND")
    public FundAccount createFundAccount(FundAccount fundAccount) {
        validateUniqueAccountNumber(fundAccount, null);
        return fundAccountRepository.save(fundAccount);
    }

    @Override
    public FundAccount getFundAccountById(Long id) {
        return fundAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fund account not found with ID: " + id));
    }

    @Override
    public FundAccount getFundAccountByAccountNumber(String accountNumber) {
        return fundAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Fund account not found with account number: " + accountNumber));
    }

    @Override
    public Page<FundAccount> searchFundAccounts(String search, FundAccountType type, Boolean active, Pageable pageable) {
        return fundAccountRepository.searchFundAccounts(search, type, active, pageable);
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE", module = "FUND")
    public FundAccount updateFundAccount(Long id, FundAccount details) {
        FundAccount existing = getFundAccountById(id);

        validateUniqueAccountNumber(details, id);

        existing.setName(details.getName());
        existing.setType(details.getType());
        existing.setAccountNumber(details.getAccountNumber());
        existing.setGlAccount(details.getGlAccount());
        if (details.getActive() != null) {
            existing.setActive(details.getActive());
        }

        return fundAccountRepository.save(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "DEACTIVATE", module = "FUND")
    public FundAccount deactivateFundAccount(Long id) {
        FundAccount account = getFundAccountById(id);
        account.setActive(false);
        return fundAccountRepository.save(account);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "FUND")
    public void deleteFundAccount(Long id) {
        FundAccount account = getFundAccountById(id);

        // Check if transactions exist for this account
        long txCount = fundTransactionRepository.searchTransactions(id, null, null, null, null, null, PageRequest.of(0, 1)).getTotalElements();
        if (txCount > 0) {
            throw new BusinessRuleException("Fund account with existing transaction history cannot be deleted. Deactivate it instead.");
        }

        fundAccountRepository.delete(account);
    }

    @Override
    public BigDecimal getTotalLiquidity() {
        return fundAccountRepository.calculateTotalLiquidity();
    }

    @Override
    public BigDecimal getTotalBalanceByType(FundAccountType type) {
        return fundAccountRepository.calculateTotalBalanceByType(type);
    }

    private void validateUniqueAccountNumber(FundAccount fundAccount, Long existingId) {
        String accountNumber = fundAccount.getAccountNumber();
        
        if (fundAccount.getType() == FundAccountType.BANK) {
            if (accountNumber == null || accountNumber.trim().isEmpty()) {
                throw new BusinessRuleException("Bank accounts must have an account number.");
            }
        }

        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return;
        }

        if (existingId == null) {
            if (fundAccountRepository.existsByAccountNumber(accountNumber)) {
                throw new DuplicateResourceException("Fund account number already exists: " + accountNumber);
            }
        } else {
            if (fundAccountRepository.existsByAccountNumberAndIdNot(accountNumber, existingId)) {
                throw new DuplicateResourceException("Fund account number already exists: " + accountNumber);
            }
        }
    }
}
