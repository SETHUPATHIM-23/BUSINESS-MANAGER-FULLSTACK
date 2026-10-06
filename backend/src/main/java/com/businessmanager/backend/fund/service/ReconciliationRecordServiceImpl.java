package com.businessmanager.backend.fund.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.DuplicateResourceException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.fund.entity.FundTransaction;
import com.businessmanager.backend.fund.entity.ReconciliationRecord;
import com.businessmanager.backend.fund.repository.FundTransactionRepository;
import com.businessmanager.backend.fund.repository.ReconciliationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReconciliationRecordServiceImpl implements ReconciliationRecordService {

    private final ReconciliationRecordRepository reconciliationRecordRepository;
    private final FundTransactionRepository fundTransactionRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE_RECONCILIATION_RECORD", module = "FUND")
    public ReconciliationRecord createReconciliationRecord(Long fundTransactionId, String bankStatementLineRef) {
        if (bankStatementLineRef == null || bankStatementLineRef.trim().isEmpty()) {
            throw new BusinessRuleException("Bank statement line reference cannot be empty.");
        }

        if (reconciliationRecordRepository.existsByBankStatementLineRef(bankStatementLineRef)) {
            throw new DuplicateResourceException("Reconciliation record already exists for bank statement line reference: " + bankStatementLineRef);
        }

        FundTransaction tx = fundTransactionRepository.findById(fundTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Fund transaction not found with ID: " + fundTransactionId));

        ReconciliationRecord record = new ReconciliationRecord();
        record.setFundTransaction(tx);
        record.setBankStatementLineRef(bankStatementLineRef);
        record.setMatched(false);

        return reconciliationRecordRepository.save(record);
    }

    @Override
    public ReconciliationRecord getReconciliationRecordById(Long id) {
        return reconciliationRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation record not found with ID: " + id));
    }

    @Override
    public Page<ReconciliationRecord> searchReconciliationRecords(Long fundAccountId, Boolean matched, String search, Pageable pageable) {
        return reconciliationRecordRepository.searchReconciliations(fundAccountId, matched, search, pageable);
    }

    @Override
    @Transactional
    @AuditAction(action = "MATCH_RECONCILIATION", module = "FUND")
    public ReconciliationRecord matchReconciliationRecord(Long id, String reconciledBy) {
        ReconciliationRecord record = getReconciliationRecordById(id);

        if (record.getMatched()) {
            throw new BusinessRuleException("Reconciliation record is already matched.");
        }

        record.setMatched(true);
        record.setReconciledAt(LocalDateTime.now());
        record.setReconciledBy(reconciledBy != null ? reconciledBy : "SYSTEM");

        return reconciliationRecordRepository.save(record);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE_RECONCILIATION_RECORD", module = "FUND")
    public void deleteReconciliationRecord(Long id) {
        ReconciliationRecord record = getReconciliationRecordById(id);

        if (record.getMatched()) {
            throw new BusinessRuleException("Matched reconciliation records cannot be deleted.");
        }

        reconciliationRecordRepository.delete(record);
    }
}
