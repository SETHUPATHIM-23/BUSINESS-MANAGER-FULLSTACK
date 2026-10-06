package com.businessmanager.backend.fund.repository;

import com.businessmanager.backend.fund.entity.FundAccount;
import com.businessmanager.backend.fund.entity.FundAccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface FundAccountRepository extends JpaRepository<FundAccount, Long> {

    Optional<FundAccount> findByAccountNumber(String accountNumber);

    Optional<FundAccount> findByGlAccountId(Long glAccountId);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByAccountNumberAndIdNot(String accountNumber, Long id);

    @Query("SELECT fa FROM FundAccount fa WHERE " +
           "(:search IS NULL OR LOWER(fa.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(fa.accountNumber) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:type IS NULL OR fa.type = :type) AND " +
           "(:active IS NULL OR fa.active = :active)")
    Page<FundAccount> searchFundAccounts(
            @Param("search") String search,
            @Param("type") FundAccountType type,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(fa.currentBalance), 0) FROM FundAccount fa WHERE fa.active = true")
    BigDecimal calculateTotalLiquidity();

    @Query("SELECT COALESCE(SUM(fa.currentBalance), 0) FROM FundAccount fa WHERE fa.active = true AND fa.type = :type")
    BigDecimal calculateTotalBalanceByType(@Param("type") FundAccountType type);
}
