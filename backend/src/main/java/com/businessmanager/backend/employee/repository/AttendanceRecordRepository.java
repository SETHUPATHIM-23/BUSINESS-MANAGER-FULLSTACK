package com.businessmanager.backend.employee.repository;

import com.businessmanager.backend.employee.entity.AttendanceRecord;
import com.businessmanager.backend.employee.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    /**
     * Look up single attendance log by employee ID and date.
     */
    Optional<AttendanceRecord> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    /**
     * Check if a log exists for employee ID and date.
     */
    boolean existsByEmployeeIdAndDate(Long employeeId, LocalDate date);

    /**
     * Fetch all logs for a specific date.
     */
    List<AttendanceRecord> findByDate(LocalDate date);

    /**
     * Fetch employee attendance history inside a date range.
     */
    List<AttendanceRecord> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    /**
     * Filtered and paginated search for attendance logs.
     */
    @Query("SELECT a FROM AttendanceRecord a WHERE " +
            "(:employeeId IS NULL OR a.employee.id = :employeeId) AND " +
            "(:startDate IS NULL OR a.date >= :startDate) AND " +
            "(:endDate IS NULL OR a.date <= :endDate) AND " +
            "(:status IS NULL OR a.status = :status)")
    Page<AttendanceRecord> searchAttendance(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") AttendanceStatus status,
            Pageable pageable
    );

    /**
     * Aggregate query counting days of specific attendance status for an employee.
     */
    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE " +
            "a.employee.id = :employeeId AND " +
            "a.status = :status AND " +
            "a.date BETWEEN :startDate AND :endDate")
    long countAttendanceByStatus(
            @Param("employeeId") Long employeeId,
            @Param("status") AttendanceStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    boolean existsByEmployeeId(Long employeeId);
}
