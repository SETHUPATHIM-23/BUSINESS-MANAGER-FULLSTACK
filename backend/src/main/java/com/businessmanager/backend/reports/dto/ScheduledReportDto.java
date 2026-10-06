package com.businessmanager.backend.reports.dto;

import com.businessmanager.backend.reports.enums.ReportRunStatus;
import com.businessmanager.backend.reports.enums.ReportType;
import com.businessmanager.backend.reports.enums.ScheduleRecurrence;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduledReportDto {
    private Long id;
    
    // Definition
    private String name;
    private String description;
    private ReportType reportType;
    private String filterConfigJson; // JSON representation of applied filters
    
    // Schedule
    private ScheduleRecurrence recurrenceRule;
    private String customCronExpression;
    
    // Delivery
    private String printTarget;
    
    // Status
    private ReportRunStatus lastRunStatus;
    private LocalDateTime lastRunTime;
    private LocalDateTime nextRunTime;
}
