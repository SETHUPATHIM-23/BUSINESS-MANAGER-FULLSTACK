package com.businessmanager.backend.reports.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import com.businessmanager.backend.product.entity.ProductCategory;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.enums.ProductStatus;
import com.businessmanager.backend.product.repository.ProductCategoryRepository;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.reports.dto.InventoryValuationReportDto;
import com.businessmanager.backend.reports.dto.SalesReportDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReportingCrossModuleVerificationTest {

    @Autowired
    private OperationalReportService operationalReportService;

    @Autowired
    private FinancialReportService financialReportService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Test
    void testSalesReportMatchesBillingLedgerWithoutDrift() {
        LocalDate today = LocalDate.now();

        Customer customer = new Customer();
        customer.setCustomerCode("CUST-REP-01");
        customer.setName("Report Test Customer");
        customer.setPhone("555-9999");
        Customer savedCustomer = customerRepository.save(customer);

        Invoice invoice1 = new Invoice();
        invoice1.setCustomer(savedCustomer);
        invoice1.setInvoiceNumber("INV-REP-101");
        invoice1.setInvoiceDate(today);
        invoice1.setDueDate(today.plusDays(30));
        invoice1.setSubtotal(new BigDecimal("500.00"));
        invoice1.setTaxTotal(new BigDecimal("50.00"));
        invoice1.setGrandTotal(new BigDecimal("550.00"));
        invoice1.setAmountPaid(new BigDecimal("0.00"));
        invoice1.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice1);

        Invoice invoice2 = new Invoice();
        invoice2.setCustomer(savedCustomer);
        invoice2.setInvoiceNumber("INV-REP-102");
        invoice2.setInvoiceDate(today);
        invoice2.setDueDate(today.plusDays(30));
        invoice2.setSubtotal(new BigDecimal("200.00"));
        invoice2.setTaxTotal(new BigDecimal("20.00"));
        invoice2.setGrandTotal(new BigDecimal("220.00"));
        invoice2.setAmountPaid(new BigDecimal("0.00"));
        invoice2.setStatus(InvoiceStatus.CANCELLED); // Should NOT be in revenue
        invoiceRepository.save(invoice2);

        SalesReportDto report = operationalReportService.generateSalesReport(today, today, savedCustomer.getId());

        // Assert no drift: It should exactly equal 550.00, completely ignoring the cancelled 220.00
        assertNotNull(report);
        assertEquals(0, new BigDecimal("550.00").compareTo(report.getTotalRevenue()), 
            "Sales Report Revenue drifted from canonical Billing reality.");
        
        // Assert invoice count is 1
        assertEquals(1, report.getInvoiceCount(), "Report should omit cancelled invoices entirely.");
    }

    @Test
    void testInventoryValuationRespectsFinancialExclusionFlag() {
        ProductCategory category = new ProductCategory();
        category.setName("Reporting Test Category");
        ProductCategory savedCategory = categoryRepository.save(category);

        Product financialProduct = new Product();
        financialProduct.setSku("SKU-FIN-1");
        financialProduct.setName("Capital Asset");
        financialProduct.setCategory(savedCategory);
        financialProduct.setCostPrice(new BigDecimal("100.00"));
        financialProduct.setBaseSellingPrice(new BigDecimal("150.00"));
        financialProduct.setStockOnHand(new BigDecimal("10"));
        financialProduct.setStatus(ProductStatus.ACTIVE);
        financialProduct.setIncludeInFinancialCalculations(true);
        financialProduct.setUnitOfMeasure("PCS");
        productRepository.save(financialProduct);

        Product excludedProduct = new Product();
        excludedProduct.setSku("SKU-EXC-2");
        excludedProduct.setName("Free Sample Material");
        excludedProduct.setCategory(savedCategory);
        excludedProduct.setCostPrice(new BigDecimal("50.00"));
        excludedProduct.setBaseSellingPrice(new BigDecimal("0.00"));
        excludedProduct.setStockOnHand(new BigDecimal("1000"));
        excludedProduct.setStatus(ProductStatus.ACTIVE);
        excludedProduct.setIncludeInFinancialCalculations(false);
        excludedProduct.setUnitOfMeasure("PCS");
        productRepository.save(excludedProduct);

        // Run the report
        InventoryValuationReportDto report = operationalReportService.generateInventoryValuationReport(savedCategory.getId());

        // Assert: Excluded product should NOT artificially inflate catalog valuation
        assertNotNull(report);
        assertEquals(1, report.getActiveProductsCount(), "Excluded products must not appear in financial valuation reports.");
        
        // Value should exactly be 10 * 100.00 = 1000.00
        assertEquals(0, new BigDecimal("1000.00").compareTo(report.getTotalCatalogValuation()), 
            "Inventory valuation failed to correctly calculate Stock * CostPrice.");

        // Assert line items
        assertTrue(report.getLines().stream().anyMatch(l -> l.getSku().equals("SKU-FIN-1")));
        assertTrue(report.getLines().stream().noneMatch(l -> l.getSku().equals("SKU-EXC-2")), 
            "Excluded products erroneously leaked into the Financial Valuation DTO lines.");
    }
}
