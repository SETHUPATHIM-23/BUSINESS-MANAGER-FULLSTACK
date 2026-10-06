package com.businessmanager.backend.reports.service;

import com.businessmanager.backend.reports.dto.AgingReportDto;
import com.businessmanager.backend.reports.dto.BalanceSheetDto;
import com.businessmanager.backend.reports.dto.ProfitAndLossDto;
import com.businessmanager.backend.reports.dto.TrialBalanceDto;

import java.time.LocalDate;

public interface FinancialReportService {

    ProfitAndLossDto generateProfitAndLoss(LocalDate startDate, LocalDate endDate);

    BalanceSheetDto generateBalanceSheet(LocalDate asOfDate);

    TrialBalanceDto generateTrialBalance(LocalDate asOfDate);

    AgingReportDto generateReceivablesAging(LocalDate asOfDate, Long customerId);

    AgingReportDto generatePayablesAging(LocalDate asOfDate, Long supplierId);
}
