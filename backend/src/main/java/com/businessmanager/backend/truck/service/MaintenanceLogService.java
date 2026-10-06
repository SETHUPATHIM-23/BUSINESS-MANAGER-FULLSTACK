package com.businessmanager.backend.truck.service;

import com.businessmanager.backend.truck.entity.MaintenanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MaintenanceLogService {
    MaintenanceLog logMaintenance(MaintenanceLog log);
    MaintenanceLog getLogById(Long id);
    void deleteLog(Long id);
    Page<MaintenanceLog> searchLogs(Long truckId, LocalDate startDate, LocalDate endDate, String type, Pageable pageable);
    BigDecimal getTotalMaintenanceCostByTruckId(Long truckId);
}
