package com.businessmanager.backend.reports.service;

import com.businessmanager.backend.printing.service.PdfGeneratorService;
import com.businessmanager.backend.printing.service.TemplateRenderService;
import com.businessmanager.backend.reports.dto.*;
import com.businessmanager.backend.settings.repository.SystemSettingsRepository;
import com.businessmanager.backend.settings.entity.SystemSettings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportExportServiceImpl implements ReportExportService {

    @Autowired
    private TemplateRenderService templateRenderService;
    
    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    private ExportData createExportData(String title, String subtitle) {
        ExportData data = new ExportData(title, subtitle);
        systemSettingsRepository.findFirstByOrderByIdAsc().ifPresent(s -> {
            if (s.getCompanyName() != null && !s.getCompanyName().trim().isEmpty()) {
                data.companyName = s.getCompanyName();
            }
            if (s.getAddress() != null && !s.getAddress().trim().isEmpty()) {
                data.companyAddress = s.getAddress();
            }
            if (s.getMobile() != null && !s.getMobile().trim().isEmpty()) {
                data.companyPhone = s.getMobile();
            } else if (s.getTelephone() != null && !s.getTelephone().trim().isEmpty()) {
                data.companyPhone = s.getTelephone();
            }
            if (s.getGstin() != null && !s.getGstin().trim().isEmpty()) {
                data.companyGstin = s.getGstin();
            }
        });
        return data;
    }

    // --- SALES REPORT ---

    @Override
    public byte[] exportSalesReportToPdf(SalesReportDto report) {
        ExportData data = new ExportData("Sales Report", "Date Range: " + report.getStartDate() + " to " + report.getEndDate());
        data.headers = List.of("Invoice Number", "Date", "Customer", "SubTotal", "Tax", "Grand Total");
        report.getLines().forEach(line -> data.addRow(
                line.getInvoiceNumber(),
                line.getInvoiceDate().toString(),
                line.getCustomerName(),
                formatAmt(line.getSubTotal()),
                formatAmt(line.getTaxTotal()),
                formatAmt(line.getGrandTotal())
        ));
        data.addSummary("Total Sales", formatAmt(report.getTotalRevenue()));
        return generatePdf(data);
    }

    @Override
    public byte[] exportSalesReportToCsv(SalesReportDto report) {
        ExportData data = new ExportData("Sales Report", "");
        data.headers = List.of("Invoice Number", "Date", "Customer", "SubTotal", "Tax", "Grand Total");
        report.getLines().forEach(line -> data.addRow(
                line.getInvoiceNumber(),
                line.getInvoiceDate().toString(),
                line.getCustomerName(),
                formatAmt(line.getSubTotal()),
                formatAmt(line.getTaxTotal()),
                formatAmt(line.getGrandTotal())
        ));
        data.addSummary("Total Sales", formatAmt(report.getTotalRevenue()));
        return generateCsv(data);
    }

    // --- PURCHASES REPORT ---

    @Override
    public byte[] exportPurchasesReportToPdf(PurchasesReportDto report) {
        ExportData data = new ExportData("Purchases Report", "Date Range: " + report.getStartDate() + " to " + report.getEndDate());
        data.headers = List.of("PO Number", "Date", "Supplier", "Total Amount", "Status");
        report.getLines().forEach(line -> data.addRow(
                line.getPoNumber(),
                line.getOrderDate().toString(),
                line.getSupplierName(),
                formatAmt(line.getTotalAmount()),
                line.getStatus()
        ));
        data.addSummary("Total Purchases", formatAmt(report.getTotalPurchases()));
        return generatePdf(data);
    }

    @Override
    public byte[] exportPurchasesReportToCsv(PurchasesReportDto report) {
        ExportData data = new ExportData("Purchases Report", "");
        data.headers = List.of("PO Number", "Date", "Supplier", "Total Amount", "Status");
        report.getLines().forEach(line -> data.addRow(
                line.getPoNumber(),
                line.getOrderDate().toString(),
                line.getSupplierName(),
                formatAmt(line.getTotalAmount()),
                line.getStatus()
        ));
        data.addSummary("Total Purchases", formatAmt(report.getTotalPurchases()));
        return generateCsv(data);
    }

    // --- RUNNING SHEET REPORT ---

    @Override
    public byte[] exportRunningSheetReportToPdf(RunningSheetReportDto report) {
        String subtitle = "Date Range: " + report.getStartDate() + " to " + report.getEndDate();
        if (report.getEmployee() != null) {
            subtitle += " | Employee: " + report.getEmployee().getName() + " (" + report.getEmployee().getCode() + ")";
        }
        ExportData data = createExportData("Employee Running Sheet Report", subtitle);
        data.headers = List.of("Date", "Employee", "Type", "Description / Payment Ref", "Worked Amount", "Settled Amount", "Running Balance");

        report.getLines().forEach(line -> {
            String desc = line.getDescription() != null ? line.getDescription() : "";
            if (line.getReferenceNo() != null && !line.getReferenceNo().isEmpty()) {
                desc += " (Ref: " + line.getReferenceNo() + ")";
            }
            if (line.getPaymentMode() != null && !line.getPaymentMode().isEmpty() && "SETTLEMENT".equalsIgnoreCase(line.getEntryType())) {
                desc += " [" + line.getPaymentMode() + "]";
            }
            data.addRow(
                    line.getDate().toString(),
                    line.getEmployeeName() + " (" + line.getEmployeeCode() + ")",
                    line.getEntryType(),
                    desc,
                    formatAmt(line.getWorkedAmount()),
                    formatAmt(line.getSettledAmount()),
                    formatAmt(line.getRunningBalance())
            );
        });

        data.addSummary("Opening Balance", formatAmt(report.getOpeningBalance()));
        data.addSummary("Total Worked Amount", formatAmt(report.getTotalWorkedAmount()));
        data.addSummary("Total Settled Amount", formatAmt(report.getTotalSettledAmount()));
        data.addSummary("Closing Net Outstanding", formatAmt(report.getClosingBalance()));
        return generatePdf(data);
    }

    @Override
    public byte[] exportRunningSheetReportToCsv(RunningSheetReportDto report) {
        String subtitle = "Date Range: " + report.getStartDate() + " to " + report.getEndDate();
        if (report.getEmployee() != null) {
            subtitle += " | Employee: " + report.getEmployee().getName() + " (" + report.getEmployee().getCode() + ")";
        }
        ExportData data = createExportData("Employee Running Sheet Report", subtitle);
        data.headers = List.of("Date", "Employee", "Type", "Description / Payment Ref", "Worked Amount", "Settled Amount", "Running Balance");

        report.getLines().forEach(line -> {
            String desc = line.getDescription() != null ? line.getDescription() : "";
            if (line.getReferenceNo() != null && !line.getReferenceNo().isEmpty()) {
                desc += " (Ref: " + line.getReferenceNo() + ")";
            }
            if (line.getPaymentMode() != null && !line.getPaymentMode().isEmpty() && "SETTLEMENT".equalsIgnoreCase(line.getEntryType())) {
                desc += " [" + line.getPaymentMode() + "]";
            }
            data.addRow(
                    line.getDate().toString(),
                    line.getEmployeeName() + " (" + line.getEmployeeCode() + ")",
                    line.getEntryType(),
                    desc,
                    formatAmt(line.getWorkedAmount()),
                    formatAmt(line.getSettledAmount()),
                    formatAmt(line.getRunningBalance())
            );
        });

        data.addSummary("Opening Balance", formatAmt(report.getOpeningBalance()));
        data.addSummary("Total Worked Amount", formatAmt(report.getTotalWorkedAmount()));
        data.addSummary("Total Settled Amount", formatAmt(report.getTotalSettledAmount()));
        data.addSummary("Closing Net Outstanding", formatAmt(report.getClosingBalance()));
        return generateCsv(data);
    }

    // --- INVENTORY VALUATION ---

    @Override
    public byte[] exportInventoryValuationToPdf(InventoryValuationReportDto report) {
        ExportData data = new ExportData("Inventory Valuation", "Generated At: " + report.getGeneratedAt());
        data.headers = List.of("SKU", "Name", "Category", "Stock", "Cost Price", "Total Value");
        report.getLines().forEach(line -> data.addRow(
                line.getSku(),
                line.getName(),
                line.getCategory(),
                formatAmt(line.getStockOnHand()),
                formatAmt(line.getCostPrice()),
                formatAmt(line.getTotalValue())
        ));
        data.addSummary("Total Catalog Valuation", formatAmt(report.getTotalCatalogValuation()));
        return generatePdf(data);
    }

    @Override
    public byte[] exportInventoryValuationToCsv(InventoryValuationReportDto report) {
        ExportData data = new ExportData("Inventory Valuation", "");
        data.headers = List.of("SKU", "Name", "Category", "Stock", "Cost Price", "Total Value");
        report.getLines().forEach(line -> data.addRow(
                line.getSku(),
                line.getName(),
                line.getCategory(),
                formatAmt(line.getStockOnHand()),
                formatAmt(line.getCostPrice()),
                formatAmt(line.getTotalValue())
        ));
        data.addSummary("Total Catalog Valuation", formatAmt(report.getTotalCatalogValuation()));
        return generateCsv(data);
    }

    // --- INCOME STATEMENT ---

    @Override
    public byte[] exportProfitAndLossToPdf(ProfitAndLossDto report) {
        ExportData data = new ExportData("Income Statement", "Date Range: " + report.getStartDate() + " to " + report.getEndDate());
        data.headers = List.of("Account Type", "Account Code", "Account Name", "Balance");
        
        report.getRevenueAccounts().forEach(a -> data.addRow("Income", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));
        report.getCogsAccounts().forEach(a -> data.addRow("COGS", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));
        report.getExpenseAccounts().forEach(a -> data.addRow("Expense", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));

        data.addSummary("Total Income", formatAmt(report.getTotalRevenue()));
        data.addSummary("Total COGS", formatAmt(report.getTotalCostOfGoodsSold()));
        data.addSummary("Gross Total", formatAmt(report.getGrossProfit()));
        data.addSummary("Total Expenses", formatAmt(report.getTotalExpenses()));
        data.addSummary("Net Total", formatAmt(report.getNetIncome()));
        return generatePdf(data);
    }

    @Override
    public byte[] exportProfitAndLossToCsv(ProfitAndLossDto report) {
        ExportData data = new ExportData("Income Statement", "Date Range: " + report.getStartDate() + " to " + report.getEndDate());
        data.headers = List.of("Account Type", "Account Code", "Account Name", "Balance");
        
        report.getRevenueAccounts().forEach(a -> data.addRow("Income", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));
        report.getCogsAccounts().forEach(a -> data.addRow("COGS", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));
        report.getExpenseAccounts().forEach(a -> data.addRow("Expense", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));

        data.addSummary("Total Income", formatAmt(report.getTotalRevenue()));
        data.addSummary("Total COGS", formatAmt(report.getTotalCostOfGoodsSold()));
        data.addSummary("Gross Total", formatAmt(report.getGrossProfit()));
        data.addSummary("Total Expenses", formatAmt(report.getTotalExpenses()));
        data.addSummary("Net Total", formatAmt(report.getNetIncome()));
        return generateCsv(data);
    }

    // --- BALANCE SHEET ---

    @Override
    public byte[] exportBalanceSheetToPdf(BalanceSheetDto report) {
        ExportData data = new ExportData("Balance Sheet", "As of: " + report.getAsOfDate());
        data.headers = List.of("Account Type", "Account Code", "Account Name", "Balance");
        
        report.getAssetAccounts().forEach(a -> data.addRow("Asset", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));
        report.getLiabilityAccounts().forEach(a -> data.addRow("Liability", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));
        report.getEquityAccounts().forEach(a -> data.addRow("Equity", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));

        data.addSummary("Total Assets", formatAmt(report.getTotalAssets()));
        data.addSummary("Total Liabilities", formatAmt(report.getTotalLiabilities()));
        data.addSummary("Total Equity", formatAmt(report.getTotalEquity()));
        return generatePdf(data);
    }

    @Override
    public byte[] exportBalanceSheetToCsv(BalanceSheetDto report) {
        ExportData data = new ExportData("Balance Sheet", "As of: " + report.getAsOfDate());
        data.headers = List.of("Account Type", "Account Code", "Account Name", "Balance");
        
        report.getAssetAccounts().forEach(a -> data.addRow("Asset", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));
        report.getLiabilityAccounts().forEach(a -> data.addRow("Liability", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));
        report.getEquityAccounts().forEach(a -> data.addRow("Equity", a.getAccountCode(), a.getAccountName(), formatAmt(a.getBalance())));

        data.addSummary("Total Assets", formatAmt(report.getTotalAssets()));
        data.addSummary("Total Liabilities", formatAmt(report.getTotalLiabilities()));
        data.addSummary("Total Equity", formatAmt(report.getTotalEquity()));
        return generateCsv(data);
    }

    // --- TRIAL BALANCE ---

    @Override
    public byte[] exportTrialBalanceToPdf(TrialBalanceDto report) {
        ExportData data = new ExportData("Trial Balance", "As of: " + report.getAsOfDate());
        data.headers = List.of("Account Code", "Account Name", "Type", "Debit", "Credit");
        report.getLines().forEach(line -> data.addRow(
                line.getAccountCode(),
                line.getAccountName(),
                line.getAccountType(),
                formatAmt(line.getDebitBalance()),
                formatAmt(line.getCreditBalance())
        ));
        data.addSummary("Total Debits", formatAmt(report.getTotalDebits()));
        data.addSummary("Total Credits", formatAmt(report.getTotalCredits()));
        data.addSummary("Balanced?", report.isBalanced() ? "Yes" : "NO");
        return generatePdf(data);
    }

    @Override
    public byte[] exportTrialBalanceToCsv(TrialBalanceDto report) {
        ExportData data = new ExportData("Trial Balance", "As of: " + report.getAsOfDate());
        data.headers = List.of("Account Code", "Account Name", "Type", "Debit", "Credit");
        report.getLines().forEach(line -> data.addRow(
                line.getAccountCode(),
                line.getAccountName(),
                line.getAccountType(),
                formatAmt(line.getDebitBalance()),
                formatAmt(line.getCreditBalance())
        ));
        data.addSummary("Total Debits", formatAmt(report.getTotalDebits()));
        data.addSummary("Total Credits", formatAmt(report.getTotalCredits()));
        data.addSummary("Balanced?", report.isBalanced() ? "Yes" : "NO");
        return generateCsv(data);
    }

    // --- AGING REPORT ---

    @Override
    public byte[] exportAgingReportToPdf(AgingReportDto report) {
        ExportData data = new ExportData(report.getType() + " Aging Report", "As of: " + report.getAsOfDate());
        data.headers = List.of("Partner", "Document", "Due Date", "Current", "1-30", "31-60", "61-90", "Over 90", "Total");
        report.getLines().forEach(line -> data.addRow(
                line.getPartnerName(),
                line.getDocumentNumber(),
                line.getDueDate().toString(),
                formatAmt(line.getCurrentAmount()),
                formatAmt(line.getDays1to30Amount()),
                formatAmt(line.getDays31to60Amount()),
                formatAmt(line.getDays61to90Amount()),
                formatAmt(line.getOver90Amount()),
                formatAmt(line.getTotalAmount())
        ));
        data.addSummary("Grand Total", formatAmt(report.getGrandTotal()));
        return generatePdf(data);
    }

    @Override
    public byte[] exportAgingReportToCsv(AgingReportDto report) {
        ExportData data = new ExportData(report.getType() + " Aging Report", "As of: " + report.getAsOfDate());
        data.headers = List.of("Partner", "Document", "Due Date", "Current", "1-30", "31-60", "61-90", "Over 90", "Total");
        report.getLines().forEach(line -> data.addRow(
                line.getPartnerName(),
                line.getDocumentNumber(),
                line.getDueDate().toString(),
                formatAmt(line.getCurrentAmount()),
                formatAmt(line.getDays1to30Amount()),
                formatAmt(line.getDays31to60Amount()),
                formatAmt(line.getDays61to90Amount()),
                formatAmt(line.getOver90Amount()),
                formatAmt(line.getTotalAmount())
        ));
        data.addSummary("Grand Total", formatAmt(report.getGrandTotal()));
        return generateCsv(data);
    }

    // --- STATEMENT REPORT ---

    @Override
    public byte[] exportStatementReportToPdf(StatementReportDto report) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("report", report);
            String html = templateRenderService.renderTemplate("statement_report", vars);
            return pdfGeneratorService.generatePdfFromHtml(html);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Statement PDF", e);
        }
    }

    @Override
    public byte[] exportStatementReportToCsv(StatementReportDto report) {
        try (StringWriter sw = new StringWriter();
             CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL)) {

            // Header metadata
            printer.printRecord((report.getStatementType() != null ? report.getStatementType() : "PARTNER") + " STATEMENT OF ACCOUNT");
            printer.printRecord("Partner:", report.getPartner() != null ? report.getPartner().getName() : "");
            printer.printRecord("Period:", report.getStartDate() + " to " + report.getEndDate());
            printer.printRecord();

            // Data header
            printer.printRecord("Date", "Ref / Doc Code", "Description", "Billed/Purchased Amount", "Paid Amount", "Running Balance");
            if (report.getLines() != null) {
                for (StatementReportDto.StatementLine line : report.getLines()) {
                    printer.printRecord(
                            line.getDate() != null ? line.getDate().toString() : "",
                            nvl(line.getDocumentCode()),
                            nvl(line.getDescription()),
                            numericAmt(line.getBilledOrPurchasedAmount()),
                            numericAmt(line.getPaidAmount()),
                            numericAmt(line.getRunningBalance())
                    );
                }
            }

            // Summary footer
            printer.printRecord();
            boolean isCustomer = "CUSTOMER".equalsIgnoreCase(report.getStatementType());
            printer.printRecord("Opening Balance", numericAmt(report.getOpeningBalance()));
            printer.printRecord(isCustomer ? "Total Billed" : "Total Purchased",
                    numericAmt(report.getTotalBilledOrPurchased()));
            printer.printRecord(isCustomer ? "Total Received" : "Total Paid",
                    numericAmt(report.getTotalPaidOrSettled()));
            printer.printRecord("Closing Balance", numericAmt(report.getClosingBalance()));

            printer.flush();
            // UTF-8 BOM + content for proper Excel auto-detection
            return withBom(sw.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Statement CSV", e);
        }
    }

    // --- OUTSTANDING SUMMARY REPORT ---

    @Override
    public byte[] exportOutstandingSummaryToPdf(OutstandingSummaryReportDto report) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("report", report);
            String html = templateRenderService.renderTemplate("outstanding_summary_report", vars);
            return pdfGeneratorService.generatePdfFromHtml(html);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Outstanding Summary PDF", e);
        }
    }

    @Override
    public byte[] exportOutstandingSummaryToCsv(OutstandingSummaryReportDto report) {
        try (StringWriter sw = new StringWriter();
             CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL)) {

            // Header metadata
            printer.printRecord((report.getReportType() != null ? report.getReportType() : "OUTSTANDING") + " SUMMARY");
            printer.printRecord("As of Date:", report.getAsOfDate() != null ? report.getAsOfDate().toString() : "");
            printer.printRecord("Total Outstanding:", numericAmt(report.getTotalOutstandingAmount()));
            printer.printRecord("Partners With Due Count:", report.getPartnersWithDueCount());
            printer.printRecord();

            // Data header
            printer.printRecord("Partner Code", "Partner Name", "Phone", "Email",
                    "Total Billed/Purchased", "Total Paid", "Outstanding Balance", "Status");
            if (report.getLines() != null) {
                for (OutstandingSummaryReportDto.OutstandingLine line : report.getLines()) {
                    printer.printRecord(
                            nvl(line.getPartnerCode()),
                            nvl(line.getPartnerName()),
                            nvl(line.getPhone()),
                            nvl(line.getEmail()),
                            numericAmt(line.getTotalBilledOrPurchased()),
                            numericAmt(line.getTotalPaid()),
                            numericAmt(line.getOutstandingBalance()),
                            nvl(line.getStatus())
                    );
                }
            }

            // Summary footer
            printer.printRecord();
            printer.printRecord("Grand Total Outstanding", numericAmt(report.getTotalOutstandingAmount()));

            printer.flush();
            return withBom(sw.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Outstanding Summary CSV", e);
        }
    }


    // =========================================================================================
    // EXPORT ENGINE CORE
    // =========================================================================================

    /** Returns a plain numeric string (e.g. "1234.56") suitable for CSV cells — Excel treats it as a number. */
    private String numericAmt(BigDecimal amt) {
        if (amt == null) return "0.00";
        return amt.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** Legacy alias used by the shared generateCsv() engine. */
    private String formatAmt(BigDecimal amt) {
        return numericAmt(amt);
    }

    /** Null-safe empty string. */
    private String nvl(String s) {
        return s != null ? s : "";
    }

    /** Prepend UTF-8 BOM so Excel opens the file with correct encoding. */
    private byte[] withBom(String csvContent) {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = csvContent.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }

    private class ExportData {
        String title;
        String subtitle;
        String companyName = "";
        String companyAddress = "";
        String companyPhone = "";
        String companyGstin = "";
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        List<String[]> summaries = new ArrayList<>();

        ExportData(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }

        void addRow(String... cols) {
            rows.add(List.of(cols));
        }

        void addSummary(String label, String value) {
            summaries.add(new String[]{label, value});
        }
    }

    private byte[] generatePdf(ExportData data) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("companyName", data.companyName);
            vars.put("companyAddress", data.companyAddress);
            vars.put("companyPhone", data.companyPhone);
            vars.put("companyGstin", data.companyGstin);
            vars.put("reportTitle", data.title);
            vars.put("reportSubtitle", data.subtitle);
            vars.put("headers", data.headers);
            vars.put("rows", data.rows);
            vars.put("summaries", data.summaries);

            String html = templateRenderService.renderTemplate("report", vars);
            return pdfGeneratorService.generatePdfFromHtml(html);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF from Template Engine", e);
        }
    }

    private byte[] generateCsv(ExportData data) {
        try (StringWriter sw = new StringWriter();
             CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL)) {

            // Company letterhead rows
            if (data.companyName != null && !data.companyName.isEmpty()) {
                printer.printRecord(data.companyName);
            }
            if (data.companyAddress != null && !data.companyAddress.isEmpty()) {
                printer.printRecord(data.companyAddress.replace("\n", ", "));
            }
            if (data.companyGstin != null && !data.companyGstin.isEmpty()) {
                printer.printRecord("GSTIN: " + data.companyGstin + " | Mobile: " + nvl(data.companyPhone));
            }
            printer.printRecord(); // blank separator

            // Report title / subtitle
            printer.printRecord(data.title);
            if (data.subtitle != null && !data.subtitle.isEmpty()) {
                printer.printRecord(data.subtitle);
            }
            printer.printRecord(); // blank separator

            // Column headers
            printer.printRecord(data.headers);

            // Data rows
            for (List<String> row : data.rows) {
                printer.printRecord(row);
            }

            // Summary footer
            if (!data.summaries.isEmpty()) {
                printer.printRecord();
                for (String[] sum : data.summaries) {
                    printer.printRecord(sum[0], sum[1]);
                }
            }

            printer.flush();
            // UTF-8 BOM so Excel auto-detects encoding (important for names with special chars)
            return withBom(sw.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }
}
