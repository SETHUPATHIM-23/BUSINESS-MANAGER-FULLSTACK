package com.businessmanager.backend.billing.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.entity.InvoiceLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoicePricingServiceImplTest {

    private InvoicePricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new InvoicePricingServiceImpl();
    }

    @Test
    void testTaxExclusiveCalculation() {
        Invoice invoice = new Invoice();
        invoice.setBillType("tax_exclusive");
        invoice.setGstPercentage(new BigDecimal("18.00"));
        invoice.setCgstRate(new BigDecimal("9.00"));
        invoice.setSgstRate(new BigDecimal("9.00"));

        InvoiceLine line1 = new InvoiceLine();
        line1.setQuantity(new BigDecimal("2.00"));
        line1.setUnitPrice(new BigDecimal("100.00"));

        InvoiceLine line2 = new InvoiceLine();
        line2.setQuantity(new BigDecimal("1.00"));
        line2.setUnitPrice(new BigDecimal("50.00"));
        line2.setDiscount(new BigDecimal("10.00")); // 50 - 10 = 40

        invoice.getLines().addAll(List.of(line1, line2));

        pricingService.calculateTotals(invoice);

        // Subtotal = 200 + 40 = 240
        assertEquals(new BigDecimal("240.00"), invoice.getSubtotal());
        
        // Tax = 240 * 18% = 43.20
        assertEquals(new BigDecimal("43.20"), invoice.getTaxTotal());
        assertEquals(new BigDecimal("21.60"), invoice.getCgst());
        assertEquals(new BigDecimal("21.60"), invoice.getSgst());

        // Grand Total = 283.20 -> rounded to 283.00
        assertEquals(new BigDecimal("283.00"), invoice.getGrandTotal());
        
        // Round off = 283.00 - 283.20 = -0.20
        assertEquals(new BigDecimal("-0.20"), invoice.getRoundOff());
    }

    @Test
    void testTaxInclusiveCalculation() {
        Invoice invoice = new Invoice();
        invoice.setBillType("tax_inclusive");
        invoice.setGstPercentage(new BigDecimal("18.00"));
        invoice.setCgstRate(new BigDecimal("9.00"));
        invoice.setSgstRate(new BigDecimal("9.00"));

        InvoiceLine line = new InvoiceLine();
        line.setQuantity(new BigDecimal("1.00"));
        line.setUnitPrice(new BigDecimal("118.00")); // Tax inclusive price
        
        invoice.getLines().add(line);

        pricingService.calculateTotals(invoice);

        // Subtotal = 118 / 1.18 = 100
        assertEquals(new BigDecimal("100.00"), invoice.getSubtotal());
        
        // Tax = 100 * 18% = 18.00
        assertEquals(new BigDecimal("18.00"), invoice.getTaxTotal());
        assertEquals(new BigDecimal("9.00"), invoice.getCgst());
        assertEquals(new BigDecimal("9.00"), invoice.getSgst());

        // Grand Total = 118.00
        assertEquals(new BigDecimal("118.00"), invoice.getGrandTotal());
        assertEquals(new BigDecimal("0.00"), invoice.getRoundOff());
    }

    @Test
    void testDeliveryChallanCalculation() {
        Invoice invoice = new Invoice();
        invoice.setBillType("delivery_challan");
        invoice.setGstPercentage(new BigDecimal("18.00"));
        invoice.setCgstRate(new BigDecimal("9.00"));
        invoice.setSgstRate(new BigDecimal("9.00"));

        InvoiceLine line = new InvoiceLine();
        line.setQuantity(new BigDecimal("5.00"));
        line.setUnitPrice(new BigDecimal("10.50"));

        invoice.getLines().add(line);

        pricingService.calculateTotals(invoice);

        // Subtotal = 5 * 10.50 = 52.50
        assertEquals(new BigDecimal("52.50"), invoice.getSubtotal());
        
        // Tax = 0.00 for challan
        assertEquals(new BigDecimal("0.00"), invoice.getTaxTotal());
        assertEquals(new BigDecimal("0.00"), invoice.getCgst());
        assertEquals(new BigDecimal("0.00"), invoice.getSgst());

        // Grand Total = 52.50 -> rounded to 53.00
        assertEquals(new BigDecimal("53.00"), invoice.getGrandTotal());
        
        // Round off = 53.00 - 52.50 = 0.50
        assertEquals(new BigDecimal("0.50"), invoice.getRoundOff());
    }
}
