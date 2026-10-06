package com.businessmanager.backend.reports.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.reports.enums.ReportRunStatus;
import com.businessmanager.backend.reports.enums.ScheduleRecurrence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_reports")
@Getter
@Setter
public class ScheduledReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_definition_id", nullable = false)
    private ReportDefinition reportDefinition;

    @Column(name = "recurrence_rule", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ScheduleRecurrence recurrenceRule;

    @Column(name = "cron_expression", length = 50)
    private String cronExpression; // Used if recurrenceRule is CUSTOM_CRON

    @Column(name = "recipient_target", length = 255)
    private String recipientTarget; // e.g. emails

    @Column(name = "print_target", length = 100)
    private String printTarget; // e.g. printer name

    @Column(name = "last_run_status", length = 20)
    @Enumerated(EnumType.STRING)
    private ReportRunStatus lastRunStatus;

    @Column(name = "last_run_time")
    private LocalDateTime lastRunTime;

    @Column(name = "next_run_time")
    private LocalDateTime nextRunTime;
}
