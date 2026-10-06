package com.businessmanager.backend.dashboard.service;

import com.businessmanager.backend.dashboard.dto.DashboardAlertsDto;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.security.dto.AuditLogEntryDto;
import com.businessmanager.backend.security.entity.AuditLogEntry;
import com.businessmanager.backend.security.mapper.AuditLogEntryMapper;
import com.businessmanager.backend.security.repository.AuditLogEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardAlertsServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogEntryRepository auditLogEntryRepository;

    @Mock
    private AuditLogEntryMapper auditLogEntryMapper;

    @Mock
    private com.businessmanager.backend.backup.service.BackupService backupService;

    @InjectMocks
    private DashboardAlertsServiceImpl dashboardAlertsService;

    @Test
    void testGetAlertsAndRecentActivity_matchesRepositories() {
        // Mock Low Stock Products
        Product p1 = new Product();
        p1.setId(1L);
        p1.setSku("TEST-001");
        p1.setName("Test Product");
        p1.setStockOnHand(new BigDecimal("5"));
        p1.setReorderLevel(new BigDecimal("10"));

        when(productRepository.findLowStockProducts()).thenReturn(List.of(p1));

        // Mock Recent Activities
        AuditLogEntry log1 = new AuditLogEntry();
        log1.setId(10L);
        log1.setModuleName("BILLING");

        AuditLogEntryDto logDto = new AuditLogEntryDto();
        logDto.setId(10L);
        logDto.setModuleName("BILLING");

        when(auditLogEntryRepository.findRecentActivitiesByModules(any(List.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log1)));

        when(auditLogEntryMapper.toDto(log1)).thenReturn(logDto);

        // Act
        DashboardAlertsDto result = dashboardAlertsService.getAlertsAndRecentActivity();

        // Assert
        assertEquals(1, result.getLowStockAlerts().size());
        assertEquals("TEST-001", result.getLowStockAlerts().get(0).getSku());
        
        assertEquals(1, result.getRecentActivities().size());
        assertEquals("BILLING", result.getRecentActivities().get(0).getModuleName());
        
        // Assert Stubs
        assertEquals("Last Backup: SUCCESS (Today 00:00 AM)", result.getMostRecentBackupStatus());
    }
}
