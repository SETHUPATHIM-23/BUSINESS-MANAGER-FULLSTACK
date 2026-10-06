package com.businessmanager.backend.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardMetricsDto {
    private BigDecimal salesToday;
    private BigDecimal salesThisMonth;
    private BigDecimal purchasesToday;
    private BigDecimal purchasesThisMonth;
    private BigDecimal cashBalance;
    private BigDecimal bankBalance;
    private AgingBucketDto receivablesAging;
    private AgingBucketDto payablesAging;

}
