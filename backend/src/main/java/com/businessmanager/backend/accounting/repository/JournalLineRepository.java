package com.businessmanager.backend.accounting.repository;

import com.businessmanager.backend.accounting.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    List<JournalLine> findByJournalEntryId(Long journalEntryId);

    List<JournalLine> findByAccountId(Long accountId);

    @Query("SELECT jl FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.journalEntry.entryDate BETWEEN :startDate AND :endDate")
    List<JournalLine> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(jl.debitAmount), 0) - COALESCE(SUM(jl.creditAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId")
    BigDecimal calculateNetBalance(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(jl.debitAmount), 0) - COALESCE(SUM(jl.creditAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.journalEntry.entryDate < :startDate")
    BigDecimal getOpeningBalance(@Param("accountId") Long accountId, @Param("startDate") LocalDate startDate);

    @Query("SELECT COALESCE(SUM(jl.debitAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.journalEntry.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal getDebitsForPeriod(@Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(jl.creditAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.journalEntry.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal getCreditsForPeriod(@Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(jl.debitAmount), 0) - COALESCE(SUM(jl.creditAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.journalEntry.entryDate <= :endDate")
    BigDecimal getBalanceUpToDate(@Param("accountId") Long accountId, @Param("endDate") LocalDate endDate);
}
