package com.businessmanager.backend.fund.repository;

import com.businessmanager.backend.fund.entity.FundTransaction;
import com.businessmanager.backend.fund.entity.FundTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FundTransactionRepository extends JpaRepository<FundTransaction, Long> {

    List<FundTransaction> findByFundAccountIdOrderByTransactionDateAscIdAsc(Long fundAccountId);

    List<FundTransaction> findByReferenceDocumentTypeAndReferenceDocumentIdOrderByTransactionDateDesc(String referenceDocumentType, Long referenceDocumentId);

    @Query("SELECT ft FROM FundTransaction ft WHERE " +
           "(:fundAccountId IS NULL OR ft.fundAccount.id = :fundAccountId OR ft.targetFundAccount.id = :fundAccountId) AND " +
           "(:type IS NULL OR ft.type = :type) AND " +
           "(:startDate IS NULL OR ft.transactionDate >= :startDate) AND " +
           "(:endDate IS NULL OR ft.transactionDate <= :endDate) AND " +
           "(:refDocType IS NULL OR ft.referenceDocumentType = :refDocType) AND " +
           "(:refDocId IS NULL OR ft.referenceDocumentId = :refDocId)")
    Page<FundTransaction> searchTransactions(
            @Param("fundAccountId") Long fundAccountId,
            @Param("type") FundTransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("refDocType") String refDocType,
            @Param("refDocId") Long refDocId,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(CASE " +
           "WHEN ft.type = 'RECEIPT' AND ft.fundAccount.id = :fundAccountId THEN ft.amount " +
           "WHEN ft.type = 'PAYMENT' AND ft.fundAccount.id = :fundAccountId THEN -ft.amount " +
           "WHEN ft.type = 'TRANSFER' AND ft.fundAccount.id = :fundAccountId THEN -ft.amount " +
           "WHEN ft.type = 'TRANSFER' AND ft.targetFundAccount.id = :fundAccountId THEN ft.amount " +
           "ELSE 0 END), 0) " +
           "FROM FundTransaction ft WHERE ft.fundAccount.id = :fundAccountId OR ft.targetFundAccount.id = :fundAccountId")
    BigDecimal calculateNetBalanceForAccount(@Param("fundAccountId") Long fundAccountId);
    @Query("SELECT COALESCE(SUM(ft.amount), 0) FROM FundTransaction ft WHERE ft.type = 'RECEIPT'")
    BigDecimal getTotalReceipts();

    @Query("SELECT COALESCE(SUM(ft.amount), 0) FROM FundTransaction ft WHERE ft.type = 'PAYMENT'")
    BigDecimal getTotalPayments();
}
