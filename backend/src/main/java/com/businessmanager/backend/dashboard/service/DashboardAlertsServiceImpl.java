package com.businessmanager.backend.dashboard.service;

import com.businessmanager.backend.backup.service.BackupService;
import com.businessmanager.backend.dashboard.dto.DashboardAlertsDto;

import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.security.entity.AuditLogEntry;
import com.businessmanager.backend.security.mapper.AuditLogEntryMapper;
import com.businessmanager.backend.security.repository.AuditLogEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardAlertsServiceImpl implements DashboardAlertsService {

    private final ProductRepository productRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final AuditLogEntryMapper auditLogEntryMapper;
    private final BackupService backupService;


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardAlerts")
    public DashboardAlertsDto getAlertsAndRecentActivity() {
        
        // 1. Low Stock Alerts disabled - returning empty list
        List<DashboardAlertsDto.ProductAlertDto> lowStockAlerts = List.of();

        // 2. Most Recent Backup Status
        String backupStatus = backupService.getMostRecentBackupStatus();


        // 4. Merged Recent Activity Feed
        // Fetch top 10 activities from Billing, Purchasing, Funds, and Inventory
        List<String> targetModules = List.of("BILLING", "PURCHASING", "FUNDS", "INVENTORY");
        List<AuditLogEntry> recentLogs = auditLogEntryRepository.findRecentActivitiesByModules(
                targetModules, 
                PageRequest.of(0, 10)
        ).getContent();
        
        var recentActivities = recentLogs.stream()
                .map(auditLogEntryMapper::toDto)
                .collect(Collectors.toList());

        return DashboardAlertsDto.builder()
                .lowStockAlerts(lowStockAlerts)
                .mostRecentBackupStatus(backupStatus)

                .recentActivities(recentActivities)
                .build();
    }
}
