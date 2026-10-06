package com.businessmanager.backend.reports.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import com.businessmanager.backend.dashboard.dto.DashboardMetricsDto;
import com.businessmanager.backend.dashboard.service.DashboardService;
import com.businessmanager.backend.reports.dto.SalesReportDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.businessmanager.backend.billing.entity.InvoiceLine;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.enums.ProductStatus;
import com.businessmanager.backend.product.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReportingAndDashboardConsistencyTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private OperationalReportService operationalReportService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testDashboardAndReportsAreMathematicallyIdentical() {
        LocalDate today = LocalDate.now();

        // 1. Setup Canonical Source Data
        Customer customer = new Customer();
        customer.setCustomerCode("CUST-CONS-01");
        customer.setName("Consistency Test Customer");
        customer.setPhone("555-1111");
        Customer savedCustomer = customerRepository.save(customer);

        Product product = new Product();
        product.setSku("SKU-CONS-01");
        product.setName("Consistency Product");
        product.setUnitOfMeasure("PCS");
        product.setCostPrice(new BigDecimal("500.00"));
        product.setBaseSellingPrice(new BigDecimal("1000.00"));
        product.setStockOnHand(new BigDecimal("100"));
        product.setStatus(ProductStatus.ACTIVE);
        product.setIncludeInFinancialCalculations(true);
        Product savedProduct = productRepository.save(product);

        // Valid Invoice
        Invoice invoice1 = new Invoice();
        invoice1.setCustomer(savedCustomer);
        invoice1.setInvoiceNumber("INV-CONS-01");
        invoice1.setInvoiceDate(today);
        invoice1.setDueDate(today.plusDays(30));
        invoice1.setSubtotal(new BigDecimal("1000.00"));
        invoice1.setTaxTotal(new BigDecimal("100.00"));
        invoice1.setGrandTotal(new BigDecimal("1100.00"));
        invoice1.setAmountPaid(new BigDecimal("0.00"));
        invoice1.setStatus(InvoiceStatus.PAID);

        InvoiceLine line1 = new InvoiceLine();
        line1.setProduct(savedProduct);
        line1.setQuantity(BigDecimal.ONE);
        line1.setUnitPrice(new BigDecimal("1000.00"));
        line1.setLineTotal(new BigDecimal("1100.00"));
        invoice1.addLine(line1);

        invoiceRepository.save(invoice1);

        // Cancelled Invoice (Should be excluded by both)
        Invoice invoice2 = new Invoice();
        invoice2.setCustomer(savedCustomer);
        invoice2.setInvoiceNumber("INV-CONS-02");
        invoice2.setInvoiceDate(today);
        invoice2.setDueDate(today.plusDays(30));
        invoice2.setSubtotal(new BigDecimal("5000.00"));
        invoice2.setTaxTotal(new BigDecimal("500.00"));
        invoice2.setGrandTotal(new BigDecimal("5500.00"));
        invoice2.setAmountPaid(new BigDecimal("0.00"));
        invoice2.setStatus(InvoiceStatus.CANCELLED);

        InvoiceLine line2 = new InvoiceLine();
        line2.setProduct(savedProduct);
        line2.setQuantity(BigDecimal.ONE);
        line2.setUnitPrice(new BigDecimal("5000.00"));
        line2.setLineTotal(new BigDecimal("5500.00"));
        invoice2.addLine(line2);

        invoiceRepository.save(invoice2);

        // 2. Fetch Dashboard Aggregate
        DashboardMetricsDto dashboard = dashboardService.getDashboardMetrics();
        BigDecimal dashboardSales = dashboard.getSalesToday();

        // 3. Fetch Operational Report Aggregate
        SalesReportDto report = operationalReportService.generateSalesReport(today, today, null);
        BigDecimal reportSales = report.getTotalRevenue();

        // 4. Verify exact match down to the cent
        assertNotNull(dashboardSales);
        assertNotNull(reportSales);
        
        // Compare using compareTo to safely handle BigDecimal scale differences (e.g., 1100.0 vs 1100.00)
        assertEquals(0, dashboardSales.compareTo(reportSales),
            "CRITICAL DRIFT DETECTED: Dashboard total (" + dashboardSales + ") does not match Sales Report total (" + reportSales + "). " +
            "This violates REP-010 constraint requiring exact mathematical consistency across modules.");
            
        // 5. Verify the actual mathematical reality against the seeded data
        assertEquals(0, new BigDecimal("1100.00").compareTo(reportSales),
            "Both systems drifted from actual database reality.");
    }
}
