package com.businessmanager.backend.employee.repository;

import com.businessmanager.backend.employee.entity.EmployeeSettlement;
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
public interface EmployeeSettlementRepository extends JpaRepository<EmployeeSettlement, Long> {

    @Query("SELECT s FROM EmployeeSettlement s WHERE " +
           "(:employeeId IS NULL OR s.employee.id = :employeeId) AND " +
           "(:startDate IS NULL OR s.settlementDate >= :startDate) AND " +
           "(:endDate IS NULL OR s.settlementDate <= :endDate)")
    Page<EmployeeSettlement> searchSettlements(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT s FROM EmployeeSettlement s WHERE " +
           "(:employeeId IS NULL OR s.employee.id = :employeeId) AND " +
           "(:startDate IS NULL OR s.settlementDate >= :startDate) AND " +
           "(:endDate IS NULL OR s.settlementDate <= :endDate) " +
           "ORDER BY s.settlementDate ASC, s.id ASC")
    List<EmployeeSettlement> findSettlementsForReport(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM EmployeeSettlement s WHERE s.employee.id = :employeeId")
    BigDecimal sumSettledAmountByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM EmployeeSettlement s WHERE s.employee.id = :employeeId AND s.settlementDate < :beforeDate")
    BigDecimal sumSettledAmountByEmployeeIdBeforeDate(@Param("employeeId") Long employeeId, @Param("beforeDate") LocalDate beforeDate);
}
