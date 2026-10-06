package com.businessmanager.backend.billing.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.entity.InvoiceLine;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class InvoicePricingServiceImpl implements InvoicePricingService {

    private static final String BILL_TYPE_DELIVERY_CHALLAN = "delivery_challan";
    private static final String BILL_TYPE_TAX_INCLUSIVE = "tax_inclusive";
    private static final String BILL_TYPE_TAX_EXCLUSIVE = "tax_exclusive"; // Default

    @Override
    public void calculateTotals(Invoice invoice) {
        if (invoice == null || invoice.getLines() == null || invoice.getLines().isEmpty()) {
            return;
        }

        String billType = invoice.getBillType();
        if (billType == null || billType.isBlank()) {
            billType = BILL_TYPE_TAX_EXCLUSIVE;
        }

        BigDecimal gstPercentage = invoice.getGstPercentage() != null 
            ? invoice.getGstPercentage() 
            : new BigDecimal("18.00");

        boolean isChallan = BILL_TYPE_DELIVERY_CHALLAN.equalsIgnoreCase(billType);
        boolean isTaxInclusive = BILL_TYPE_TAX_INCLUSIVE.equalsIgnoreCase(billType);

        BigDecimal subtotal = BigDecimal.ZERO;
        
        // 1. Calculate lines
        for (InvoiceLine line : invoice.getLines()) {
            BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
            BigDecimal rate = line.getUnitPrice() != null ? line.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal discount = line.getDiscount() != null ? line.getDiscount() : BigDecimal.ZERO;

            if (qty.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("Quantity and rate must be non-negative");
            }

            BigDecimal grossAmount = qty.multiply(rate);
            BigDecimal netAmount = grossAmount.subtract(discount);
            
            BigDecimal lineTotal;

            if (isChallan || !isTaxInclusive) {
                // Tax exclusive or challan: line total is just net amount
                lineTotal = netAmount.setScale(2, RoundingMode.HALF_UP);
            } else {
                // Tax inclusive: rate includes tax, we need to extract base amount
                BigDecimal denominator = BigDecimal.ONE.add(gstPercentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                lineTotal = netAmount.divide(denominator, 2, RoundingMode.HALF_UP);
            }

            line.setLineTotal(lineTotal);
            line.setTaxAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)); // We calculate tax at document level now to match billingex
            
            subtotal = subtotal.add(lineTotal);
        }
        
        invoice.setSubtotal(subtotal);

        // 2. Document level tax
        BigDecimal taxTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal cgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal igst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        
        if (!isChallan) {
            BigDecimal cRate = invoice.getCgstRate() != null ? invoice.getCgstRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal sRate = invoice.getSgstRate() != null ? invoice.getSgstRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal iRate = invoice.getIgstRate() != null ? invoice.getIgstRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            
            cgst = subtotal.multiply(cRate).setScale(2, RoundingMode.HALF_UP);
            sgst = subtotal.multiply(sRate).setScale(2, RoundingMode.HALF_UP);
            igst = subtotal.multiply(iRate).setScale(2, RoundingMode.HALF_UP);
            
            taxTotal = cgst.add(sgst).add(igst);
        }

        invoice.setTaxTotal(taxTotal);
        invoice.setCgst(cgst);
        invoice.setSgst(sgst);
        invoice.setIgst(igst);

        // 3. Grand total and Round off
        BigDecimal mathematicalTotal = subtotal.add(taxTotal);
        BigDecimal roundedGrandTotal = mathematicalTotal.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        BigDecimal roundOff = roundedGrandTotal.subtract(mathematicalTotal);

        invoice.setRoundOff(roundOff);
        invoice.setGrandTotal(roundedGrandTotal);
    }
}
