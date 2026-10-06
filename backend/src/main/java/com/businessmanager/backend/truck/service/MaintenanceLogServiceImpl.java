package com.businessmanager.backend.truck.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.truck.entity.MaintenanceLog;
import com.businessmanager.backend.truck.entity.Truck;
import com.businessmanager.backend.truck.repository.MaintenanceLogRepository;
import com.businessmanager.backend.truck.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceLogServiceImpl implements MaintenanceLogService {

    private final MaintenanceLogRepository maintenanceLogRepository;
    private final TruckRepository truckRepository;

    @Override
    @Transactional
    @AuditAction(action = "LOG_MAINTENANCE", module = "MAINTENANCE_LOG")
    public MaintenanceLog logMaintenance(MaintenanceLog log) {
        MaintenanceLog saved = maintenanceLogRepository.save(log);

        // Update truck lastServiceDate if applicable
        Truck truck = log.getTruck();
        if (truck != null && (truck.getLastServiceDate() == null || log.getDate().isAfter(truck.getLastServiceDate()))) {
            truck.setLastServiceDate(log.getDate());
            truckRepository.save(truck);
        }

        return saved;
    }

    @Override
    public MaintenanceLog getLogById(Long id) {
        return maintenanceLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance log not found with ID: " + id));
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "MAINTENANCE_LOG")
    public void deleteLog(Long id) {
        MaintenanceLog log = getLogById(id);
        maintenanceLogRepository.delete(log);
    }

    @Override
    public Page<MaintenanceLog> searchLogs(Long truckId, LocalDate startDate, LocalDate endDate, String type, Pageable pageable) {
        return maintenanceLogRepository.searchLogs(truckId, startDate, endDate, type, pageable);
    }

    @Override
    public BigDecimal getTotalMaintenanceCostByTruckId(Long truckId) {
        return maintenanceLogRepository.getTotalMaintenanceCostByTruckId(truckId);
    }
}
