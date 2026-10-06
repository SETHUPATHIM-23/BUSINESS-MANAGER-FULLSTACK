package com.businessmanager.backend.employee.repository;

import com.businessmanager.backend.employee.entity.EmployeeWorkEntry;
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
public interface EmployeeWorkEntryRepository extends JpaRepository<EmployeeWorkEntry, Long> {

    @Query("SELECT w FROM EmployeeWorkEntry w WHERE " +
           "(:employeeId IS NULL OR w.employee.id = :employeeId) AND " +
           "(:startDate IS NULL OR w.date >= :startDate) AND " +
           "(:endDate IS NULL OR w.date <= :endDate)")
    Page<EmployeeWorkEntry> searchWorkEntries(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT w FROM EmployeeWorkEntry w WHERE " +
           "(:employeeId IS NULL OR w.employee.id = :employeeId) AND " +
           "(:startDate IS NULL OR w.date >= :startDate) AND " +
           "(:endDate IS NULL OR w.date <= :endDate) " +
           "ORDER BY w.date ASC, w.id ASC")
    List<EmployeeWorkEntry> findWorkEntriesForReport(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(w.workedAmount), 0) FROM EmployeeWorkEntry w WHERE w.employee.id = :employeeId")
    BigDecimal sumWorkedAmountByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT COALESCE(SUM(w.workedAmount), 0) FROM EmployeeWorkEntry w WHERE w.employee.id = :employeeId AND w.date < :beforeDate")
    BigDecimal sumWorkedAmountByEmployeeIdBeforeDate(@Param("employeeId") Long employeeId, @Param("beforeDate") LocalDate beforeDate);
}
