package com.businessmanager.backend.fund.service;

import com.businessmanager.backend.fund.entity.FundTransaction;
import com.businessmanager.backend.fund.entity.FundTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FundTransactionService {

    FundTransaction recordAutomaticTransaction(
            Long fundAccountId,
            FundTransactionType type,
            BigDecimal amount,
            LocalDate date,
            String refDocType,
            Long refDocId,
            String description
    );

    FundTransaction transferFunds(
            Long sourceAccountId,
            Long targetAccountId,
            BigDecimal amount,
            LocalDate date,
            String description
    );

    FundTransaction reverseTransaction(Long originalTransactionId, String reason);

    void deleteTransaction(Long id); // Throws BusinessRuleException per FUND-060

    FundTransaction getTransactionById(Long id);

    Page<FundTransaction> searchTransactions(
            Long fundAccountId,
            FundTransactionType type,
            LocalDate startDate,
            LocalDate endDate,
            String refDocType,
            Long refDocId,
            Pageable pageable
    );

    // Document posting helpers (FUND-020)
    FundTransaction recordCustomerPayment(Long fundAccountId, Long invoiceId, BigDecimal amount, LocalDate date);

    FundTransaction recordSupplierPayment(Long fundAccountId, Long purchaseOrderId, BigDecimal amount, LocalDate date);

    FundTransaction recordPayrollDisbursement(Long fundAccountId, Long employeeId, BigDecimal amount, LocalDate date);

    /** Total of all RECEIPT transactions (money received across all accounts). */
    BigDecimal getTotalReceipts();

    /** Total of all PAYMENT transactions (money sent across all accounts). */
    BigDecimal getTotalPayments();
}
