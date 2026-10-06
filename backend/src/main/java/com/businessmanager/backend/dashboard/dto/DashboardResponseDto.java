package com.businessmanager.backend.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponseDto {
    private List<String> enabledWidgets;
    private DashboardMetricsDto metrics;
    private DashboardAlertsDto alerts;
}
