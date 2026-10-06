package com.businessmanager.backend.accounting.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.AccountMapping;
import com.businessmanager.backend.accounting.enums.AccountType;
import com.businessmanager.backend.accounting.repository.AccountMappingRepository;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountMappingInitializer implements CommandLineRunner {

    private final AccountMappingRepository accountMappingRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Verifying and initializing Chart of Accounts mappings...");

        Map<String, String> requiredMappings = new LinkedHashMap<>();
        requiredMappings.put("CASH", "1010");
        requiredMappings.put("BANK", "1020");
        requiredMappings.put("ACCOUNTS_RECEIVABLE", "1100");
        requiredMappings.put("INVENTORY_ASSET", "1200");
        requiredMappings.put("TAX_RECEIVABLE", "1300");
        requiredMappings.put("ACCOUNTS_PAYABLE", "2100");
        requiredMappings.put("ACCRUED_PURCHASES", "2150");
        requiredMappings.put("TAX_PAYABLE", "2200");
        requiredMappings.put("EQUITY", "3000");
        requiredMappings.put("SALES_REVENUE", "4000");
        requiredMappings.put("COST_OF_GOODS_SOLD", "5000");
        requiredMappings.put("SUNDRY_EXPENSE", "5100");
        requiredMappings.put("ROUNDING_ADJUSTMENT", "5100");

        for (Map.Entry<String, String> entry : requiredMappings.entrySet()) {
            String key = entry.getKey();
            String code = entry.getValue();

            if (accountMappingRepository.findByMappingKey(key).isEmpty()) {
                Account targetAccount = resolveOrCreateAccount(code, key);
                if (targetAccount != null) {
                    AccountMapping mapping = new AccountMapping();
                    mapping.setMappingKey(key);
                    mapping.setAccount(targetAccount);
                    mapping.setCreatedBy("system");
                    mapping.setUpdatedBy("system");
                    accountMappingRepository.save(mapping);
                    log.info("Auto-configured account mapping: {} -> Account code {}", key, targetAccount.getCode());
                }
            }
        }
    }

    private Account resolveOrCreateAccount(String defaultCode, String mappingKey) {
        // 1. Try finding by code
        var accOpt = accountRepository.findByCode(defaultCode);
        if (accOpt.isPresent()) return accOpt.get();

        // 2. Try finding by account type or name heuristic
        AccountType targetType = inferType(mappingKey);
        var typeMatches = accountRepository.findAll().stream()
                .filter(a -> a.getType() == targetType)
                .findFirst();
        if (typeMatches.isPresent()) return typeMatches.get();

        // 3. Create missing default account if not present
        Account newAccount = new Account();
        newAccount.setCode(defaultCode);
        newAccount.setName(formatAccountName(mappingKey));
        newAccount.setType(targetType);
        newAccount.setActive(true);
        return accountRepository.save(newAccount);
    }

    private AccountType inferType(String key) {
        if (key.contains("RECEIVABLE") || key.equals("CASH") || key.equals("BANK") || key.contains("INVENTORY")) {
            return AccountType.ASSET;
        }
        if (key.contains("PAYABLE") || key.contains("PURCHASES")) {
            return AccountType.LIABILITY;
        }
        if (key.contains("SALES") || key.contains("REVENUE")) {
            return AccountType.REVENUE;
        }
        if (key.contains("EQUITY")) {
            return AccountType.EQUITY;
        }
        return AccountType.EXPENSE;
    }

    private String formatAccountName(String key) {
        String name = key.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
