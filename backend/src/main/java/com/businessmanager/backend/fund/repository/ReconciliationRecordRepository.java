package com.businessmanager.backend.fund.repository;

import com.businessmanager.backend.fund.entity.ReconciliationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReconciliationRecordRepository extends JpaRepository<ReconciliationRecord, Long> {

    Optional<ReconciliationRecord> findByFundTransactionId(Long fundTransactionId);

    Optional<ReconciliationRecord> findByBankStatementLineRef(String bankStatementLineRef);

    boolean existsByBankStatementLineRef(String bankStatementLineRef);

    @Query("SELECT r FROM ReconciliationRecord r WHERE " +
           "(:fundAccountId IS NULL OR r.fundTransaction.fundAccount.id = :fundAccountId) AND " +
           "(:matched IS NULL OR r.matched = :matched) AND " +
           "(:search IS NULL OR LOWER(r.bankStatementLineRef) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ReconciliationRecord> searchReconciliations(
            @Param("fundAccountId") Long fundAccountId,
            @Param("matched") Boolean matched,
            @Param("search") String search,
            Pageable pageable
    );
}
