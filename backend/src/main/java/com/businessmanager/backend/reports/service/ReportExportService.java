package com.businessmanager.backend.reports.service;

import com.businessmanager.backend.reports.dto.*;

public interface ReportExportService {

    byte[] exportSalesReportToPdf(SalesReportDto report);
    byte[] exportSalesReportToCsv(SalesReportDto report);

    byte[] exportPurchasesReportToPdf(PurchasesReportDto report);
    byte[] exportPurchasesReportToCsv(PurchasesReportDto report);

    byte[] exportInventoryValuationToPdf(InventoryValuationReportDto report);
    byte[] exportInventoryValuationToCsv(InventoryValuationReportDto report);

    byte[] exportProfitAndLossToPdf(ProfitAndLossDto report);
    byte[] exportProfitAndLossToCsv(ProfitAndLossDto report);

    byte[] exportBalanceSheetToPdf(BalanceSheetDto report);
    byte[] exportBalanceSheetToCsv(BalanceSheetDto report);

    byte[] exportTrialBalanceToPdf(TrialBalanceDto report);
    byte[] exportTrialBalanceToCsv(TrialBalanceDto report);

    byte[] exportAgingReportToPdf(AgingReportDto report);
    byte[] exportAgingReportToCsv(AgingReportDto report);

    byte[] exportStatementReportToPdf(StatementReportDto report);
    byte[] exportStatementReportToCsv(StatementReportDto report);

    byte[] exportOutstandingSummaryToPdf(OutstandingSummaryReportDto report);
    byte[] exportOutstandingSummaryToCsv(OutstandingSummaryReportDto report);

    byte[] exportRunningSheetReportToPdf(RunningSheetReportDto report);
    byte[] exportRunningSheetReportToCsv(RunningSheetReportDto report);
}
