package com.businessmanager.backend.dashboard.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.dashboard.dto.AgingBucketDto;
import com.businessmanager.backend.dashboard.dto.DashboardMetricsDto;
import com.businessmanager.backend.fund.entity.FundAccountType;
import com.businessmanager.backend.fund.repository.FundAccountRepository;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import com.businessmanager.backend.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private FundAccountRepository fundAccountRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void testGetDashboardMetrics_matchesSourceRepositories() {
        // Mock data
        BigDecimal mockSalesToday = new BigDecimal("1500.00");
        BigDecimal mockSalesMonth = new BigDecimal("15000.00");
        BigDecimal mockPurchasesToday = new BigDecimal("800.00");
        BigDecimal mockPurchasesMonth = new BigDecimal("8000.00");
        BigDecimal mockCash = new BigDecimal("5000.00");
        BigDecimal mockBank = new BigDecimal("25000.00");
        BigDecimal mockPayables = new BigDecimal("3000.00");

        when(invoiceRepository.calculateSalesTotalForDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(0);
                    LocalDate end = invocation.getArgument(1);
                    return start.equals(end) ? mockSalesToday : mockSalesMonth;
                });

        when(purchaseOrderRepository.calculatePurchasesTotalForDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(0);
                    LocalDate end = invocation.getArgument(1);
                    return start.equals(end) ? mockPurchasesToday : mockPurchasesMonth;
                });

        when(fundAccountRepository.calculateTotalBalanceByType(FundAccountType.CASH)).thenReturn(mockCash);
        when(fundAccountRepository.calculateTotalBalanceByType(FundAccountType.BANK)).thenReturn(mockBank);
        when(supplierRepository.calculateTotalOutstandingPayables()).thenReturn(mockPayables);

        // Receivables aging mock
        Invoice inv1 = new Invoice();
        inv1.setGrandTotal(new BigDecimal("1000"));
        inv1.setAmountPaid(new BigDecimal("200"));
        inv1.setDueDate(LocalDate.now().plusDays(5)); // Current

        Invoice inv2 = new Invoice();
        inv2.setGrandTotal(new BigDecimal("500"));
        inv2.setAmountPaid(BigDecimal.ZERO);
        inv2.setDueDate(LocalDate.now().minusDays(10)); // 30 days

        when(invoiceRepository.findAllOutstandingInvoices()).thenReturn(List.of(inv1, inv2));

        // Act
        DashboardMetricsDto result = dashboardService.getDashboardMetrics();

        // Assert basic KPIs
        assertEquals(mockSalesToday, result.getSalesToday());
        assertEquals(mockSalesMonth, result.getSalesThisMonth());
        assertEquals(mockPurchasesToday, result.getPurchasesToday());
        assertEquals(mockPurchasesMonth, result.getPurchasesThisMonth());
        assertEquals(mockCash, result.getCashBalance());
        assertEquals(mockBank, result.getBankBalance());

        // Assert Payables
        AgingBucketDto payables = result.getPayablesAging();
        assertEquals(mockPayables, payables.getCurrent());
        assertEquals(mockPayables, payables.getTotal());
        assertEquals(BigDecimal.ZERO, payables.getDays30());

        // Assert Receivables
        AgingBucketDto receivables = result.getReceivablesAging();
        assertEquals(new BigDecimal("800"), receivables.getCurrent());
        assertEquals(new BigDecimal("500"), receivables.getDays30());
        assertEquals(BigDecimal.ZERO, receivables.getDays60());
        assertEquals(new BigDecimal("1300"), receivables.getTotal());
    }
}
