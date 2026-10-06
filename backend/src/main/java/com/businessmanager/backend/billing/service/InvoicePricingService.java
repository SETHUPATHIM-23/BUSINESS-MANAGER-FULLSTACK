package com.businessmanager.backend.billing.service;

import com.businessmanager.backend.billing.entity.Invoice;

public interface InvoicePricingService {
    /**
     * Calculates the line totals and document-level totals (taxes, rounding, subtotal, grandTotal)
     * for a given invoice based on its billType and line items.
     * Updates the fields on the passed Invoice instance directly.
     *
     * @param invoice the invoice to compute totals for
     */
    void calculateTotals(Invoice invoice);
}
