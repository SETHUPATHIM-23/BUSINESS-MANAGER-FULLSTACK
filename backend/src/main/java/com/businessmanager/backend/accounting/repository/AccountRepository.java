package com.businessmanager.backend.accounting.repository;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.enums.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT a FROM Account a WHERE " +
           "(:search IS NULL OR LOWER(a.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:type IS NULL OR a.type = :type)")
    Page<Account> searchAccounts(@Param("search") String search, @Param("type") AccountType type, Pageable pageable);

    @Query("SELECT COALESCE(SUM(jl.debitAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId")
    BigDecimal getTotalDebits(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(jl.creditAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId")
    BigDecimal getTotalCredits(@Param("accountId") Long accountId);
}
