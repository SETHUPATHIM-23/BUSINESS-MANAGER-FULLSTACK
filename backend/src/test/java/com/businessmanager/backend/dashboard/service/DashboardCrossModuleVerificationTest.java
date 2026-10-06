package com.businessmanager.backend.dashboard.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import com.businessmanager.backend.dashboard.dto.DashboardMetricsDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DashboardCrossModuleVerificationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void verifyNoDriftBetweenBillingAndDashboardSales() {
        // Arrange: Capture initial state
        DashboardMetricsDto initialMetrics = dashboardService.getDashboardMetrics();
        BigDecimal initialSalesToday = initialMetrics.getSalesToday() != null ? initialMetrics.getSalesToday() : BigDecimal.ZERO;

        // Arrange: Create a new verified Customer
        Customer customer = new Customer();
        customer.setName("Test Dashboard Verification Customer");
        customer.setPhone("555-0000");
        Customer savedCustomer = customerRepository.save(customer);

        // Arrange: Create a new Invoice in the Billing Module representing a sale today
        Invoice invoice = new Invoice();
        invoice.setCustomer(savedCustomer);
        invoice.setInvoiceNumber("INV-VERIFY-100");
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setSubtotal(new BigDecimal("1000.00"));
        invoice.setTaxTotal(new BigDecimal("100.00"));
        invoice.setGrandTotal(new BigDecimal("1100.00"));
        invoice.setAmountPaid(new BigDecimal("0.00"));
        invoice.setStatus(InvoiceStatus.PAID); // Only completed/paid should count as "Sales", assuming our query considers completed

        invoiceRepository.save(invoice);

        // Act: Re-fetch dashboard metrics
        DashboardMetricsDto updatedMetrics = dashboardService.getDashboardMetrics();
        BigDecimal updatedSalesToday = updatedMetrics.getSalesToday() != null ? updatedMetrics.getSalesToday() : BigDecimal.ZERO;

        // Assert: The Dashboard MUST exactly reflect the new invoice added to the Billing module without drift
        BigDecimal expectedSales = initialSalesToday.add(new BigDecimal("1100.00"));
        
        // Use compareTo for precise BigDecimal equality irrespective of scale
        assertEquals(0, expectedSales.compareTo(updatedSalesToday), 
            "CRITICAL DRIFT DETECTED: The Dashboard's 'Sales Today' KPI did not exactly match the Billing Module's source of truth.");
    }
}
