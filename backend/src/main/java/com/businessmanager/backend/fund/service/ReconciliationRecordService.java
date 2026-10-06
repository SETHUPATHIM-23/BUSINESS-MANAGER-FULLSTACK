package com.businessmanager.backend.fund.service;

import com.businessmanager.backend.fund.entity.ReconciliationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReconciliationRecordService {

    ReconciliationRecord createReconciliationRecord(Long fundTransactionId, String bankStatementLineRef);

    ReconciliationRecord getReconciliationRecordById(Long id);

    Page<ReconciliationRecord> searchReconciliationRecords(
            Long fundAccountId,
            Boolean matched,
            String search,
            Pageable pageable
    );

    ReconciliationRecord matchReconciliationRecord(Long id, String reconciledBy);

    void deleteReconciliationRecord(Long id);
}
