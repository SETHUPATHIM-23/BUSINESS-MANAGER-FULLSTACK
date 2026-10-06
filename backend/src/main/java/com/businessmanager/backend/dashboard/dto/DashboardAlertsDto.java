package com.businessmanager.backend.dashboard.dto;

import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.security.dto.AuditLogEntryDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardAlertsDto {
    private List<ProductAlertDto> lowStockAlerts;
    private String mostRecentBackupStatus;

    private List<AuditLogEntryDto> recentActivities;

    @Data
    @Builder
    public static class ProductAlertDto {
        private Long id;
        private String sku;
        private String name;
        private java.math.BigDecimal stockOnHand;
        private java.math.BigDecimal reorderLevel;
    }
}
