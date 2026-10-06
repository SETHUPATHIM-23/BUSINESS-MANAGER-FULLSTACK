package com.businessmanager.backend.dashboard.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.dashboard.dto.AgingBucketDto;
import com.businessmanager.backend.dashboard.dto.DashboardMetricsDto;
import com.businessmanager.backend.fund.entity.FundAccountType;
import com.businessmanager.backend.fund.repository.FundAccountRepository;

import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final FundAccountRepository fundAccountRepository;


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardMetrics")
    public DashboardMetricsDto getDashboardMetrics() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // 1. Sales Totals
        BigDecimal salesToday = invoiceRepository.calculateSalesTotalForDateRange(today, today);
        BigDecimal salesThisMonth = invoiceRepository.calculateSalesTotalForDateRange(startOfMonth, endOfMonth);

        // 2. Purchase Totals
        BigDecimal purchasesToday = purchaseOrderRepository.calculatePurchasesTotalForDateRange(today, today);
        BigDecimal purchasesThisMonth = purchaseOrderRepository.calculatePurchasesTotalForDateRange(startOfMonth, endOfMonth);

        // 3. Fund Balances
        BigDecimal cashBalance = fundAccountRepository.calculateTotalBalanceByType(FundAccountType.CASH);
        BigDecimal bankBalance = fundAccountRepository.calculateTotalBalanceByType(FundAccountType.BANK);

        // 4. Receivables Aging
        List<Invoice> outstandingInvoices = invoiceRepository.findAllOutstandingInvoices();
        AgingBucketDto receivablesAging = calculateReceivablesAging(outstandingInvoices, today);

        // 5. Payables Aging (DASH-060)
        // Effective due date per PO = orderDate + supplier.paymentTermsDays.
        // We use POs that are RECEIVED/PARTIALLY_RECEIVED (AP liability posted) as outstanding payables.
        List<PurchaseOrder> outstandingPOs = purchaseOrderRepository.findAllOutstandingPurchaseOrders();
        AgingBucketDto payablesAging = calculatePayablesAging(outstandingPOs, today);



        return DashboardMetricsDto.builder()
                .salesToday(salesToday)
                .salesThisMonth(salesThisMonth)
                .purchasesToday(purchasesToday)
                .purchasesThisMonth(purchasesThisMonth)
                .cashBalance(cashBalance)
                .bankBalance(bankBalance)
                .receivablesAging(receivablesAging)
                .payablesAging(payablesAging)

                .build();
    }

    private AgingBucketDto calculateReceivablesAging(List<Invoice> outstandingInvoices, LocalDate today) {
        BigDecimal current = BigDecimal.ZERO;
        BigDecimal days30 = BigDecimal.ZERO;
        BigDecimal days60 = BigDecimal.ZERO;
        BigDecimal days90Plus = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        List<AgingBucketDto.CustomerAgingDetailDto> details = new java.util.ArrayList<>();

        for (Invoice invoice : outstandingInvoices) {
            BigDecimal outstandingAmount = invoice.getGrandTotal().subtract(
                invoice.getAmountPaid() != null ? invoice.getAmountPaid() : BigDecimal.ZERO
            );
            if (outstandingAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

            total = total.add(outstandingAmount);

            LocalDate dueDate = invoice.getDueDate() != null ? invoice.getDueDate() : invoice.getInvoiceDate().plusDays(30);
            long ageInDays = ChronoUnit.DAYS.between(invoice.getInvoiceDate(), today);
            long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);

            String statusCategory;
            BigDecimal cAmt = BigDecimal.ZERO;
            BigDecimal d30Amt = BigDecimal.ZERO;
            BigDecimal d60Amt = BigDecimal.ZERO;
            BigDecimal d90Amt = BigDecimal.ZERO;

            if (dueDate.isAfter(today) || dueDate.isEqual(today)) {
                current = current.add(outstandingAmount);
                cAmt = outstandingAmount;
                statusCategory = "Current";
            } else {
                if (daysOverdue <= 30) {
                    days30 = days30.add(outstandingAmount);
                    d30Amt = outstandingAmount;
                    statusCategory = "1-30 Days";
                } else if (daysOverdue <= 60) {
                    days60 = days60.add(outstandingAmount);
                    d60Amt = outstandingAmount;
                    statusCategory = "31-60 Days";
                } else {
                    days90Plus = days90Plus.add(outstandingAmount);
                    d90Amt = outstandingAmount;
                    statusCategory = "90+ Days";
                }
            }

            details.add(AgingBucketDto.CustomerAgingDetailDto.builder()
                    .customerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)
                    .customerName(invoice.getCustomer() != null ? invoice.getCustomer().getName() : "Unknown Customer")
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .invoiceDate(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toString() : "")
                    .dueDate(dueDate.toString())
                    .outstandingAmount(outstandingAmount)
                    .ageInDays(ageInDays)
                    .statusCategory(statusCategory)
                    .currentAmount(cAmt)
                    .days30Amount(d30Amt)
                    .days60Amount(d60Amt)
                    .days90PlusAmount(d90Amt)
                    .build());
        }

        return AgingBucketDto.builder()
                .current(current)
                .days30(days30)
                .days60(days60)
                .days90Plus(days90Plus)
                .total(total)
                .details(details)
                .build();
    }

    /**
     * DASH-060: Payables aging using PO.orderDate + supplier.paymentTermsDays as effective due date.
     * Buckets outstanding PO line totals into the standard 4 aging buckets.
     */
    private AgingBucketDto calculatePayablesAging(List<PurchaseOrder> outstandingPOs, LocalDate today) {
        BigDecimal current = BigDecimal.ZERO;
        BigDecimal days30 = BigDecimal.ZERO;
        BigDecimal days60 = BigDecimal.ZERO;
        BigDecimal days90Plus = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (PurchaseOrder po : outstandingPOs) {
            // Use the repository sum to avoid N+1 lazy-loading of po.getLines()
            BigDecimal poAmountRaw = purchaseOrderRepository.sumLineTotalsByPoId(po.getId());
            if (poAmountRaw == null || poAmountRaw.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal poAmount = poAmountRaw.subtract(po.getAmountPaid() != null ? po.getAmountPaid() : BigDecimal.ZERO);
            if (poAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

            total = total.add(poAmount);

            // Effective due date = order date + supplier payment terms
            LocalDate effectiveDueDate = po.getOrderDate()
                    .plusDays(po.getSupplier().getPaymentTermsDays());

            if (!effectiveDueDate.isBefore(today)) {
                current = current.add(poAmount);
            } else {
                long daysOverdue = ChronoUnit.DAYS.between(effectiveDueDate, today);
                if (daysOverdue <= 30) {
                    days30 = days30.add(poAmount);
                } else if (daysOverdue <= 60) {
                    days60 = days60.add(poAmount);
                } else {
                    days90Plus = days90Plus.add(poAmount);
                }
            }
        }

        return AgingBucketDto.builder()
                .current(current)
                .days30(days30)
                .days60(days60)
                .days90Plus(days90Plus)
                .total(total)
                .build();
    }
}
