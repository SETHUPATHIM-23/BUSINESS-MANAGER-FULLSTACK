package com.businessmanager.backend.fund.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.fund.entity.FundAccount;
import com.businessmanager.backend.fund.entity.FundAccountType;
import com.businessmanager.backend.fund.entity.FundTransaction;
import com.businessmanager.backend.fund.entity.FundTransactionType;
import com.businessmanager.backend.fund.repository.FundAccountRepository;
import com.businessmanager.backend.fund.repository.FundTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FundTransactionServiceImpl implements FundTransactionService {

    private final FundTransactionRepository fundTransactionRepository;
    private final FundAccountRepository fundAccountRepository;

    @Override
    @Transactional
    @AuditAction(action = "RECORD_TRANSACTION", module = "FUND")
    public FundTransaction recordAutomaticTransaction(
            Long fundAccountId,
            FundTransactionType type,
            BigDecimal amount,
            LocalDate date,
            String refDocType,
            Long refDocId,
            String description) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Transaction amount must be a positive number.");
        }

        FundAccount account = fundAccountRepository.findById(fundAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Fund account not found with ID: " + fundAccountId));

        if (!account.getActive()) {
            throw new BusinessRuleException("Cannot post transaction to an inactive fund account.");
        }

        // Balance updates & overdraft checks
        if (type == FundTransactionType.RECEIPT) {
            account.setCurrentBalance(account.getCurrentBalance().add(amount));
        } else if (type == FundTransactionType.PAYMENT) {
            if (account.getType() == FundAccountType.CASH && account.getCurrentBalance().compareTo(amount) < 0) {
                throw new BusinessRuleException("Insufficient funds in cash account '" + account.getName() + "'. Current balance: " + account.getCurrentBalance());
            }
            account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
        }

        fundAccountRepository.save(account);

        FundTransaction tx = new FundTransaction();
        tx.setFundAccount(account);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setTransactionDate(date != null ? date : LocalDate.now());
        tx.setReferenceDocumentType(refDocType);
        tx.setReferenceDocumentId(refDocId);
        tx.setDescription(description);

        return fundTransactionRepository.save(tx);
    }

    @Override
    @Transactional
    @AuditAction(action = "TRANSFER_FUNDS", module = "FUND")
    public FundTransaction transferFunds(
            Long sourceAccountId,
            Long targetAccountId,
            BigDecimal amount,
            LocalDate date,
            String description) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Transfer amount must be a positive number.");
        }

        if (sourceAccountId.equals(targetAccountId)) {
            throw new BusinessRuleException("Source and target fund accounts must be different.");
        }

        FundAccount source = fundAccountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Source fund account not found with ID: " + sourceAccountId));
        FundAccount target = fundAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Target fund account not found with ID: " + targetAccountId));

        if (!source.getActive()) {
            throw new BusinessRuleException("Source fund account is inactive.");
        }
        if (!target.getActive()) {
            throw new BusinessRuleException("Target fund account is inactive.");
        }

        if (source.getCurrentBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient funds in source account '" + source.getName() + "'. Current balance: " + source.getCurrentBalance());
        }

        // Atomic balance update
        source.setCurrentBalance(source.getCurrentBalance().subtract(amount));
        target.setCurrentBalance(target.getCurrentBalance().add(amount));

        fundAccountRepository.save(source);
        fundAccountRepository.save(target);

        FundTransaction tx = new FundTransaction();
        tx.setFundAccount(source);
        tx.setTargetFundAccount(target);
        tx.setType(FundTransactionType.TRANSFER);
        tx.setAmount(amount);
        tx.setTransactionDate(date != null ? date : LocalDate.now());
        tx.setDescription(description != null ? description : "Inter-fund transfer from " + source.getName() + " to " + target.getName());

        return fundTransactionRepository.save(tx);
    }

    @Override
    @Transactional
    @AuditAction(action = "REVERSE_TRANSACTION", module = "FUND")
    public FundTransaction reverseTransaction(Long originalTransactionId, String reason) {
        FundTransaction orig = getTransactionById(originalTransactionId);

        String memo = "Reversal of Tx #" + originalTransactionId + (reason != null ? ": " + reason : "");

        if (orig.getType() == FundTransactionType.RECEIPT) {
            return recordAutomaticTransaction(
                    orig.getFundAccount().getId(),
                    FundTransactionType.PAYMENT,
                    orig.getAmount(),
                    LocalDate.now(),
                    "REVERSAL",
                    orig.getId(),
                    memo
            );
        } else if (orig.getType() == FundTransactionType.PAYMENT) {
            return recordAutomaticTransaction(
                    orig.getFundAccount().getId(),
                    FundTransactionType.RECEIPT,
                    orig.getAmount(),
                    LocalDate.now(),
                    "REVERSAL",
                    orig.getId(),
                    memo
            );
        } else {
            // Reversing a transfer
            return transferFunds(
                    orig.getTargetFundAccount().getId(),
                    orig.getFundAccount().getId(),
                    orig.getAmount(),
                    LocalDate.now(),
                    memo
            );
        }
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        // Enforce immutability requirement (FUND-060)
        throw new BusinessRuleException("Posted fund transactions are immutable and cannot be deleted. Use reverseTransaction instead.");
    }

    @Override
    public FundTransaction getTransactionById(Long id) {
        return fundTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fund transaction not found with ID: " + id));
    }

    @Override
    public Page<FundTransaction> searchTransactions(
            Long fundAccountId,
            FundTransactionType type,
            LocalDate startDate,
            LocalDate endDate,
            String refDocType,
            Long refDocId,
            Pageable pageable) {
        return fundTransactionRepository.searchTransactions(fundAccountId, type, startDate, endDate, refDocType, refDocId, pageable);
    }

    @Override
    @Transactional
    @AuditAction(action = "RECORD_CUSTOMER_PAYMENT", module = "FUND")
    public FundTransaction recordCustomerPayment(Long fundAccountId, Long invoiceId, BigDecimal amount, LocalDate date) {
        return recordAutomaticTransaction(
                fundAccountId,
                FundTransactionType.RECEIPT,
                amount,
                date,
                "CUSTOMER_INVOICE",
                invoiceId,
                "Customer Payment for Invoice #" + invoiceId
        );
    }

    @Override
    @Transactional
    @AuditAction(action = "RECORD_SUPPLIER_PAYMENT", module = "FUND")
    public FundTransaction recordSupplierPayment(Long fundAccountId, Long purchaseOrderId, BigDecimal amount, LocalDate date) {
        return recordAutomaticTransaction(
                fundAccountId,
                FundTransactionType.PAYMENT,
                amount,
                date,
                "PURCHASE_ORDER",
                purchaseOrderId,
                "Supplier Payment for Purchase Order #" + purchaseOrderId
        );
    }

    @Override
    @Transactional
    @AuditAction(action = "RECORD_PAYROLL_DISBURSEMENT", module = "FUND")
    public FundTransaction recordPayrollDisbursement(Long fundAccountId, Long employeeId, BigDecimal amount, LocalDate date) {
        return recordAutomaticTransaction(
                fundAccountId,
                FundTransactionType.PAYMENT,
                amount,
                date,
                "PAYROLL",
                employeeId,
                "Salary Disbursement for Employee #" + employeeId
        );
    }

    @Override
    public BigDecimal getTotalReceipts() {
        BigDecimal result = fundTransactionRepository.getTotalReceipts();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalPayments() {
        BigDecimal result = fundTransactionRepository.getTotalPayments();
        return result != null ? result : BigDecimal.ZERO;
    }
}
