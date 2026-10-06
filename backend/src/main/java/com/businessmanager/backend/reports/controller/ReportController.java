package com.businessmanager.backend.reports.controller;


import com.businessmanager.backend.reports.dto.*;
import com.businessmanager.backend.reports.service.FinancialReportService;
import com.businessmanager.backend.reports.service.OperationalReportService;
import com.businessmanager.backend.reports.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final OperationalReportService operationalReportService;
    private final FinancialReportService financialReportService;
    private final ReportExportService reportExportService;

    // --- JSON Endpoints ---

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('REPORTING_READ')")
    public ResponseEntity<SalesReportDto> getSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long customerId) {
        
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);
        return ResponseEntity.ok(operationalReportService.generateSalesReport(start, end, customerId));
    }

    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('REPORTING_READ')")
    public ResponseEntity<PurchasesReportDto> getPurchasesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long supplierId) {
        
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);
        return ResponseEntity.ok(operationalReportService.generatePurchasesReport(start, end, supplierId));
    }

    @GetMapping("/inventory-valuation")
    @PreAuthorize("hasAuthority('REPORTING_READ')")
    public ResponseEntity<InventoryValuationReportDto> getInventoryValuation(
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(operationalReportService.generateInventoryValuationReport(categoryId));
    }

    @GetMapping("/customer-statement")
    @PreAuthorize("hasAuthority('REPORTING_READ')")
    public ResponseEntity<StatementReportDto> getCustomerStatement(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(operationalReportService.generateCustomerStatement(customerId, startDate, endDate));
    }

    @GetMapping("/supplier-statement")
    @PreAuthorize("hasAuthority('REPORTING_READ')")
    public ResponseEntity<StatementReportDto> getSupplierStatement(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(operationalReportService.generateSupplierStatement(supplierId, startDate, endDate));
    }

    @GetMapping("/customer-outstanding-summary")
    @PreAuthorize("hasAuthority('REPORTING_READ')")
    public ResponseEntity<OutstandingSummaryReportDto> getCustomerOutstandingSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(operationalReportService.generateCustomerOutstandingSummary(asOfDate));
    }

    @GetMapping("/supplier-outstanding-summary")
    @PreAuthorize("hasAuthority('REPORTING_READ')")
    public ResponseEntity<OutstandingSummaryReportDto> getSupplierOutstandingSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(operationalReportService.generateSupplierOutstandingSummary(asOfDate));
    }

    @GetMapping("/running-sheet")
    @PreAuthorize("hasAuthority('REPORTING_READ')")
    public ResponseEntity<RunningSheetReportDto> getRunningSheetReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long employeeId) {
        return ResponseEntity.ok(operationalReportService.generateRunningSheetReport(startDate, endDate, employeeId));
    }

    @GetMapping("/financial/pnl")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<ProfitAndLossDto> getProfitAndLoss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);
        return ResponseEntity.ok(financialReportService.generateProfitAndLoss(start, end));
    }

    @GetMapping("/financial/balance-sheet")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<BalanceSheetDto> getBalanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(financialReportService.generateBalanceSheet(asOfDate != null ? asOfDate : LocalDate.now()));
    }

    @GetMapping("/financial/trial-balance")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<TrialBalanceDto> getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(financialReportService.generateTrialBalance(asOfDate != null ? asOfDate : LocalDate.now()));
    }

    @GetMapping("/financial/aging/receivables")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<AgingReportDto> getReceivablesAging(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Long customerId) {
        return ResponseEntity.ok(financialReportService.generateReceivablesAging(asOfDate != null ? asOfDate : LocalDate.now(), customerId));
    }

    @GetMapping("/financial/aging/payables")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<AgingReportDto> getPayablesAging(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Long supplierId) {
        return ResponseEntity.ok(financialReportService.generatePayablesAging(asOfDate != null ? asOfDate : LocalDate.now(), supplierId));
    }

    // --- Dynamic Export Endpoint ---

    @GetMapping("/{type}/export")
    @PreAuthorize("hasAuthority('REPORTING_READ')")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable String type,
            @RequestParam(defaultValue = "PDF") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);
        LocalDate asOf = asOfDate != null ? asOfDate : LocalDate.now();
        boolean isCsv = "CSV".equalsIgnoreCase(format);
        String extension = isCsv ? "csv" : "pdf";
        String contentType = isCsv ? "text/csv" : "application/pdf";

        byte[] fileData = null;
        String filename = type.replace('/', '_') + "_report." + extension;

        switch (type) {
            case "running-sheet": {
                RunningSheetReportDto dto = operationalReportService.generateRunningSheetReport(start, end, employeeId);
                fileData = isCsv ? reportExportService.exportRunningSheetReportToCsv(dto) : reportExportService.exportRunningSheetReportToPdf(dto);
                break;
            }
            case "sales": {
                SalesReportDto dto = operationalReportService.generateSalesReport(start, end, customerId);
                fileData = isCsv ? reportExportService.exportSalesReportToCsv(dto) : reportExportService.exportSalesReportToPdf(dto);
                break;
            }
            case "purchases": {
                PurchasesReportDto dto = operationalReportService.generatePurchasesReport(start, end, supplierId);
                fileData = isCsv ? reportExportService.exportPurchasesReportToCsv(dto) : reportExportService.exportPurchasesReportToPdf(dto);
                break;
            }
            case "inventory-valuation": {
                InventoryValuationReportDto dto = operationalReportService.generateInventoryValuationReport(categoryId);
                fileData = isCsv ? reportExportService.exportInventoryValuationToCsv(dto) : reportExportService.exportInventoryValuationToPdf(dto);
                break;
            }
            case "customer-statement": {
                StatementReportDto dto = operationalReportService.generateCustomerStatement(customerId, start, end);
                fileData = isCsv ? reportExportService.exportStatementReportToCsv(dto) : reportExportService.exportStatementReportToPdf(dto);
                break;
            }
            case "supplier-statement": {
                StatementReportDto dto = operationalReportService.generateSupplierStatement(supplierId, start, end);
                fileData = isCsv ? reportExportService.exportStatementReportToCsv(dto) : reportExportService.exportStatementReportToPdf(dto);
                break;
            }
            case "customer-outstanding-summary": {
                OutstandingSummaryReportDto dto = operationalReportService.generateCustomerOutstandingSummary(asOf);
                fileData = isCsv ? reportExportService.exportOutstandingSummaryToCsv(dto) : reportExportService.exportOutstandingSummaryToPdf(dto);
                break;
            }
            case "supplier-outstanding-summary": {
                OutstandingSummaryReportDto dto = operationalReportService.generateSupplierOutstandingSummary(asOf);
                fileData = isCsv ? reportExportService.exportOutstandingSummaryToCsv(dto) : reportExportService.exportOutstandingSummaryToPdf(dto);
                break;
            }
            case "financial/pnl": {
                ProfitAndLossDto dto = financialReportService.generateProfitAndLoss(start, end);
                fileData = isCsv ? reportExportService.exportProfitAndLossToCsv(dto) : reportExportService.exportProfitAndLossToPdf(dto);
                break;
            }
            case "financial/balance-sheet": {
                BalanceSheetDto dto = financialReportService.generateBalanceSheet(asOf);
                fileData = isCsv ? reportExportService.exportBalanceSheetToCsv(dto) : reportExportService.exportBalanceSheetToPdf(dto);
                break;
            }
            case "financial/trial-balance": {
                TrialBalanceDto dto = financialReportService.generateTrialBalance(asOf);
                fileData = isCsv ? reportExportService.exportTrialBalanceToCsv(dto) : reportExportService.exportTrialBalanceToPdf(dto);
                break;
            }
            case "financial/aging/receivables": {
                AgingReportDto dto = financialReportService.generateReceivablesAging(asOf, customerId);
                fileData = isCsv ? reportExportService.exportAgingReportToCsv(dto) : reportExportService.exportAgingReportToPdf(dto);
                break;
            }
            case "financial/aging/payables": {
                AgingReportDto dto = financialReportService.generatePayablesAging(asOf, supplierId);
                fileData = isCsv ? reportExportService.exportAgingReportToCsv(dto) : reportExportService.exportAgingReportToPdf(dto);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported report type for export: " + type);
        }

        return buildFileResponse(fileData, filename, contentType);
    }

    private ResponseEntity<byte[]> buildFileResponse(byte[] data, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        return ResponseEntity.ok().headers(headers).body(data);
    }


}
