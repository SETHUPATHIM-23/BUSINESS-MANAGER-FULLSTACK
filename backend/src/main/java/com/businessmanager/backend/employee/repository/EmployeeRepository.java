package com.businessmanager.backend.employee.repository;

import com.businessmanager.backend.employee.entity.Employee;
import com.businessmanager.backend.employee.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Look up employee by unique employee code (business key).
     */
    Optional<Employee> findByEmployeeCode(String employeeCode);

    /**
     * Filtered and paginated search for employee records.
     */
    @Query("SELECT e FROM Employee e WHERE " +
            "(:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:code IS NULL OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
            "(:department IS NULL OR LOWER(e.department) LIKE LOWER(CONCAT('%', :department, '%'))) AND " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:locationId IS NULL OR e.location.id = :locationId)")
    Page<Employee> searchEmployees(
            @Param("name") String name,
            @Param("code") String code,
            @Param("department") String department,
            @Param("status") EmployeeStatus status,
            @Param("locationId") Long locationId,
            Pageable pageable
    );

    /**
     * Check if an employee code already exists.
     */
    boolean existsByEmployeeCode(String employeeCode);

    /**
     * Find all employees by status.
     */
    java.util.List<Employee> findAllByStatus(EmployeeStatus status);
}
