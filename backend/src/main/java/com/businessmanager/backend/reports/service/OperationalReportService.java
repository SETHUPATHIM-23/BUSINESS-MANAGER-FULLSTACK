package com.businessmanager.backend.reports.service;

import com.businessmanager.backend.reports.dto.*;

import java.time.LocalDate;

public interface OperationalReportService {
    
    SalesReportDto generateSalesReport(LocalDate startDate, LocalDate endDate, Long customerId);
    
    PurchasesReportDto generatePurchasesReport(LocalDate startDate, LocalDate endDate, Long supplierId);
    
    InventoryValuationReportDto generateInventoryValuationReport(Long categoryId);

    StatementReportDto generateCustomerStatement(Long customerId, LocalDate startDate, LocalDate endDate);

    StatementReportDto generateSupplierStatement(Long supplierId, LocalDate startDate, LocalDate endDate);

    OutstandingSummaryReportDto generateCustomerOutstandingSummary(LocalDate asOfDate);

    OutstandingSummaryReportDto generateSupplierOutstandingSummary(LocalDate asOfDate);

    RunningSheetReportDto generateRunningSheetReport(LocalDate startDate, LocalDate endDate, Long employeeId);
}
