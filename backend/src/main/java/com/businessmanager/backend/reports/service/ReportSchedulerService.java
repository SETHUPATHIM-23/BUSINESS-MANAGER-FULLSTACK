package com.businessmanager.backend.reports.service;


import com.businessmanager.backend.reports.dto.InventoryValuationReportDto;
import com.businessmanager.backend.reports.dto.ProfitAndLossDto;
import com.businessmanager.backend.reports.dto.SalesReportDto;
import com.businessmanager.backend.reports.entity.ScheduledReport;
import com.businessmanager.backend.reports.enums.ReportRunStatus;
import com.businessmanager.backend.reports.repository.ScheduledReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportSchedulerService {

    private final ScheduledReportRepository scheduledReportRepository;
    private final OperationalReportService operationalReportService;
    private final FinancialReportService financialReportService;
    private final ReportExportService reportExportService;

    // Run every minute to check for due reports
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processScheduledReports() {
        LocalDateTime now = LocalDateTime.now();
        // Eligible statuses: PENDING (newly created, first run), FAILED (retry), SUCCESS (next cycle)
        List<ScheduledReport> dueReports = scheduledReportRepository.findDueReports(
                now,
                List.of(ReportRunStatus.PENDING, ReportRunStatus.SUCCESS, ReportRunStatus.FAILED)
        );
        
        if (!dueReports.isEmpty()) {
            log.info("Found {} scheduled reports due for processing.", dueReports.size());
        }

        for (ScheduledReport sr : dueReports) {
            try {
                log.info("Processing scheduled report ID: {}", sr.getId());
                sr.setLastRunStatus(ReportRunStatus.PENDING);
                scheduledReportRepository.saveAndFlush(sr);

                byte[] pdfData = generateReportPdf(sr);

                // If email target, we would send an email here...
                
                sr.setLastRunStatus(ReportRunStatus.SUCCESS);
                sr.setLastRunTime(now);
                sr.setNextRunTime(calculateNextRunTime(sr, now));

            } catch (Exception e) {
                log.error("Failed to process scheduled report ID: {}", sr.getId(), e);
                sr.setLastRunStatus(ReportRunStatus.FAILED);
            }
            scheduledReportRepository.save(sr);
        }
    }

    private byte[] generateReportPdf(ScheduledReport sr) {
        // Defaults to last 30 days for scheduled operational reports if no exact date is configured
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);

        switch (sr.getReportDefinition().getReportType()) {
            case SALES_SUMMARY:
                SalesReportDto salesReport = operationalReportService.generateSalesReport(start, end, null);
                return reportExportService.exportSalesReportToPdf(salesReport);
            case INVENTORY_VALUATION:
                InventoryValuationReportDto invReport = operationalReportService.generateInventoryValuationReport(null);
                return reportExportService.exportInventoryValuationToPdf(invReport);
            case FINANCIAL_BALANCE:
                ProfitAndLossDto pnlReport = financialReportService.generateProfitAndLoss(start, end);
                return reportExportService.exportProfitAndLossToPdf(pnlReport);
            default:
                throw new UnsupportedOperationException("Scheduled generation not yet supported for type: " + sr.getReportDefinition().getReportType());
        }
    }

    private LocalDateTime calculateNextRunTime(ScheduledReport sr, LocalDateTime now) {
        switch (sr.getRecurrenceRule()) {
            case DAILY:
                return now.plusDays(1);
            case WEEKLY:
                return now.plusWeeks(1);
            case MONTHLY:
                return now.plusMonths(1);
            case CUSTOM_CRON:
                // Assuming an external cron parser in a real implementation
                return now.plusHours(1); // Stub
            default:
                return now.plusDays(1);
        }
    }
}
