package com.businessmanager.backend.truck.repository;

import com.businessmanager.backend.truck.entity.MaintenanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long> {

    List<MaintenanceLog> findByTruckId(Long truckId);

    boolean existsByTruckId(Long truckId);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceLog m WHERE m.truck.id = :truckId")
    BigDecimal getTotalMaintenanceCostByTruckId(@Param("truckId") Long truckId);

    @Query("SELECT m FROM MaintenanceLog m WHERE " +
            "(:truckId IS NULL OR m.truck.id = :truckId) AND " +
            "(:startDate IS NULL OR m.date >= :startDate) AND " +
            "(:endDate IS NULL OR m.date <= :endDate) AND " +
            "(:type IS NULL OR LOWER(m.type) LIKE LOWER(CONCAT('%', :type, '%')))")
    Page<MaintenanceLog> searchLogs(
            @Param("truckId") Long truckId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("type") String type,
            Pageable pageable
    );
}
