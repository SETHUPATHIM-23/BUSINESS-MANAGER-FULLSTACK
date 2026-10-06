package com.businessmanager.backend.reports.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.enums.AccountType;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.repository.JournalLineRepository;
import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import com.businessmanager.backend.reports.dto.AgingReportDto;
import com.businessmanager.backend.reports.dto.BalanceSheetDto;
import com.businessmanager.backend.reports.dto.ProfitAndLossDto;
import com.businessmanager.backend.reports.dto.TrialBalanceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService {

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfitAndLossDto generateProfitAndLoss(LocalDate startDate, LocalDate endDate) {
        List<Account> accounts = accountRepository.findAll();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCogs = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        List<ProfitAndLossDto.AccountLine> revenueAccounts = new ArrayList<>();
        List<ProfitAndLossDto.AccountLine> cogsAccounts = new ArrayList<>();
        List<ProfitAndLossDto.AccountLine> expenseAccounts = new ArrayList<>();

        for (Account acc : accounts) {
            BigDecimal debits = journalLineRepository.getDebitsForPeriod(acc.getId(), startDate, endDate);
            BigDecimal credits = journalLineRepository.getCreditsForPeriod(acc.getId(), startDate, endDate);

            if (acc.getType() == AccountType.INCOME) {
                // Income: Balance = Credits - Debits
                BigDecimal balance = credits.subtract(debits);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    revenueAccounts.add(buildAccountLine(acc, balance));
                    totalRevenue = totalRevenue.add(balance);
                }
            } else if (acc.getType() == AccountType.EXPENSE) {
                // Expense: Balance = Debits - Credits
                BigDecimal balance = debits.subtract(credits);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    if (acc.getName().toLowerCase().contains("cost of goods")) {
                        cogsAccounts.add(buildAccountLine(acc, balance));
                        totalCogs = totalCogs.add(balance);
                    } else {
                        expenseAccounts.add(buildAccountLine(acc, balance));
                        totalExpenses = totalExpenses.add(balance);
                    }
                }
            }
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalCogs);
        BigDecimal netIncome = grossProfit.subtract(totalExpenses);

        return ProfitAndLossDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalRevenue(totalRevenue)
                .totalCostOfGoodsSold(totalCogs)
                .grossProfit(grossProfit)
                .totalExpenses(totalExpenses)
                .netIncome(netIncome)
                .revenueAccounts(revenueAccounts)
                .cogsAccounts(cogsAccounts)
                .expenseAccounts(expenseAccounts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceSheetDto generateBalanceSheet(LocalDate asOfDate) {
        List<Account> accounts = accountRepository.findAll();

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;

        List<BalanceSheetDto.AccountLine> assetAccounts = new ArrayList<>();
        List<BalanceSheetDto.AccountLine> liabilityAccounts = new ArrayList<>();
        List<BalanceSheetDto.AccountLine> equityAccounts = new ArrayList<>();

        BigDecimal retainedEarnings = BigDecimal.ZERO;

        for (Account acc : accounts) {
            // Balance up to date = Debits - Credits
            BigDecimal netBalance = journalLineRepository.getBalanceUpToDate(acc.getId(), asOfDate);
            
            if (acc.getType() == AccountType.ASSET) {
                if (netBalance.compareTo(BigDecimal.ZERO) != 0) {
                    assetAccounts.add(buildBsLine(acc, netBalance));
                    totalAssets = totalAssets.add(netBalance);
                }
            } else if (acc.getType() == AccountType.LIABILITY) {
                // Liability balance = Credits - Debits
                BigDecimal bal = netBalance.negate();
                if (bal.compareTo(BigDecimal.ZERO) != 0) {
                    liabilityAccounts.add(buildBsLine(acc, bal));
                    totalLiabilities = totalLiabilities.add(bal);
                }
            } else if (acc.getType() == AccountType.EQUITY) {
                // Equity balance = Credits - Debits
                BigDecimal bal = netBalance.negate();
                if (bal.compareTo(BigDecimal.ZERO) != 0) {
                    equityAccounts.add(buildBsLine(acc, bal));
                    totalEquity = totalEquity.add(bal);
                }
            } else if (acc.getType() == AccountType.INCOME) {
                // Income adds to Retained Earnings (Credits - Debits)
                retainedEarnings = retainedEarnings.add(netBalance.negate());
            } else if (acc.getType() == AccountType.EXPENSE) {
                // Expense subtracts from Retained Earnings (Debits - Credits) -> subtract netBalance which is (D - C)
                retainedEarnings = retainedEarnings.subtract(netBalance);
            }
        }

        // Add calculated retained earnings to Equity
        if (retainedEarnings.compareTo(BigDecimal.ZERO) != 0) {
            equityAccounts.add(BalanceSheetDto.AccountLine.builder()
                    .accountCode("3999")
                    .accountName("Retained Earnings (Calculated)")
                    .balance(retainedEarnings)
                    .build());
            totalEquity = totalEquity.add(retainedEarnings);
        }

        return BalanceSheetDto.builder()
                .asOfDate(asOfDate)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .totalEquity(totalEquity)
                .assetAccounts(assetAccounts)
                .liabilityAccounts(liabilityAccounts)
                .equityAccounts(equityAccounts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TrialBalanceDto generateTrialBalance(LocalDate asOfDate) {
        List<Account> accounts = accountRepository.findAll();

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        List<TrialBalanceDto.TrialBalanceLine> lines = new ArrayList<>();

        for (Account acc : accounts) {
            // Net balance = Debits - Credits
            BigDecimal netBalance = journalLineRepository.getBalanceUpToDate(acc.getId(), asOfDate);
            
            if (netBalance.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal debitBal = BigDecimal.ZERO;
            BigDecimal creditBal = BigDecimal.ZERO;

            if (netBalance.compareTo(BigDecimal.ZERO) > 0) {
                debitBal = netBalance;
                totalDebits = totalDebits.add(debitBal);
            } else {
                creditBal = netBalance.negate();
                totalCredits = totalCredits.add(creditBal);
            }

            lines.add(TrialBalanceDto.TrialBalanceLine.builder()
                    .accountCode(acc.getCode())
                    .accountName(acc.getName())
                    .accountType(acc.getType().name())
                    .debitBalance(debitBal)
                    .creditBalance(creditBal)
                    .build());
        }

        return TrialBalanceDto.builder()
                .asOfDate(asOfDate)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .isBalanced(totalDebits.compareTo(totalCredits) == 0)
                .lines(lines)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AgingReportDto generateReceivablesAging(LocalDate asOfDate, Long customerId) {
        List<Invoice> outstanding = invoiceRepository.findAllOutstandingInvoices();
        if (customerId != null) {
            outstanding = outstanding.stream().filter(i -> i.getCustomer().getId().equals(customerId)).collect(Collectors.toList());
        }

        return buildAgingReport(
                "RECEIVABLES",
                asOfDate,
                outstanding.stream().map(i -> {
                    BigDecimal due = i.getGrandTotal().subtract(i.getAmountPaid() != null ? i.getAmountPaid() : BigDecimal.ZERO);
                    return new RawAgingRecord(
                            i.getCustomer().getName(),
                            i.getInvoiceNumber(),
                            i.getDueDate() != null ? i.getDueDate() : i.getInvoiceDate().plusDays(30),
                            due
                    );
                }).collect(Collectors.toList())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AgingReportDto generatePayablesAging(LocalDate asOfDate, Long supplierId) {
        List<PurchaseOrder> allPos = purchaseOrderRepository.findAll();
        if (supplierId != null) {
            allPos = allPos.stream().filter(po -> po.getSupplier().getId().equals(supplierId)).collect(Collectors.toList());
        }
        
        List<RawAgingRecord> records = new ArrayList<>();
        
        for (PurchaseOrder po : allPos) {
            if (po.getStatus().name().equals("CANCELLED")) {
                continue;
            }
            
            BigDecimal total = po.getLines().stream()
                    .map(l -> l.getLineTotal() != null ? l.getLineTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
            BigDecimal paid = po.getAmountPaid() != null ? po.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal due = total.subtract(paid);
            
            if (due.compareTo(BigDecimal.ZERO) > 0) {
                records.add(new RawAgingRecord(
                        po.getSupplier().getName(),
                        po.getPoNumber(),
                        po.getOrderDate().plusDays(30), // Default 30 days due
                        due
                ));
            }
        }

        return buildAgingReport("PAYABLES", asOfDate, records);
    }

    // --- Helpers ---

    private ProfitAndLossDto.AccountLine buildAccountLine(Account acc, BigDecimal bal) {
        return ProfitAndLossDto.AccountLine.builder()
                .accountCode(acc.getCode())
                .accountName(acc.getName())
                .balance(bal)
                .build();
    }
    
    private BalanceSheetDto.AccountLine buildBsLine(Account acc, BigDecimal bal) {
        return BalanceSheetDto.AccountLine.builder()
                .accountCode(acc.getCode())
                .accountName(acc.getName())
                .balance(bal)
                .build();
    }

    private static class RawAgingRecord {
        String partner;
        String docNumber;
        LocalDate dueDate;
        BigDecimal amount;

        RawAgingRecord(String partner, String docNumber, LocalDate dueDate, BigDecimal amount) {
            this.partner = partner;
            this.docNumber = docNumber;
            this.dueDate = dueDate;
            this.amount = amount;
        }
    }

    private AgingReportDto buildAgingReport(String type, LocalDate asOfDate, List<RawAgingRecord> records) {
        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal total1to30 = BigDecimal.ZERO;
        BigDecimal total31to60 = BigDecimal.ZERO;
        BigDecimal total61to90 = BigDecimal.ZERO;
        BigDecimal totalOver90 = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        List<AgingReportDto.AgingLine> lines = new ArrayList<>();

        for (RawAgingRecord rec : records) {
            long daysOverdue = ChronoUnit.DAYS.between(rec.dueDate, asOfDate);
            
            BigDecimal currentAmount = BigDecimal.ZERO;
            BigDecimal days1to30Amount = BigDecimal.ZERO;
            BigDecimal days31to60Amount = BigDecimal.ZERO;
            BigDecimal days61to90Amount = BigDecimal.ZERO;
            BigDecimal over90Amount = BigDecimal.ZERO;

            if (daysOverdue <= 0) {
                currentAmount = rec.amount;
                totalCurrent = totalCurrent.add(rec.amount);
            } else if (daysOverdue <= 30) {
                days1to30Amount = rec.amount;
                total1to30 = total1to30.add(rec.amount);
            } else if (daysOverdue <= 60) {
                days31to60Amount = rec.amount;
                total31to60 = total31to60.add(rec.amount);
            } else if (daysOverdue <= 90) {
                days61to90Amount = rec.amount;
                total61to90 = total61to90.add(rec.amount);
            } else {
                over90Amount = rec.amount;
                totalOver90 = totalOver90.add(rec.amount);
            }

            grandTotal = grandTotal.add(rec.amount);

            lines.add(AgingReportDto.AgingLine.builder()
                    .partnerName(rec.partner)
                    .documentNumber(rec.docNumber)
                    .dueDate(rec.dueDate)
                    .currentAmount(currentAmount)
                    .days1to30Amount(days1to30Amount)
                    .days31to60Amount(days31to60Amount)
                    .days61to90Amount(days61to90Amount)
                    .over90Amount(over90Amount)
                    .totalAmount(rec.amount)
                    .build());
        }

        return AgingReportDto.builder()
                .asOfDate(asOfDate)
                .type(type)
                .totalCurrent(totalCurrent)
                .total1to30(total1to30)
                .total31to60(total31to60)
                .total61to90(total61to90)
                .totalOver90(totalOver90)
                .grandTotal(grandTotal)
                .lines(lines)
                .build();
    }
}
