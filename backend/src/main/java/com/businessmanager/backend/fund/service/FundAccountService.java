package com.businessmanager.backend.fund.service;

import com.businessmanager.backend.fund.entity.FundAccount;
import com.businessmanager.backend.fund.entity.FundAccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface FundAccountService {

    FundAccount createFundAccount(FundAccount fundAccount);

    FundAccount getFundAccountById(Long id);

    FundAccount getFundAccountByAccountNumber(String accountNumber);

    Page<FundAccount> searchFundAccounts(String search, FundAccountType type, Boolean active, Pageable pageable);

    FundAccount updateFundAccount(Long id, FundAccount details);

    FundAccount deactivateFundAccount(Long id);

    void deleteFundAccount(Long id);

    BigDecimal getTotalLiquidity();

    BigDecimal getTotalBalanceByType(FundAccountType type);
}
