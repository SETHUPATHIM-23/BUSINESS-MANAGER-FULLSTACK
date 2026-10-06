package com.businessmanager.backend.purchasing.service;

import com.businessmanager.backend.purchasing.dto.SupplierInvoiceRequest;

public interface ThreeWayMatchService {
    void processSupplierInvoice(SupplierInvoiceRequest request);
}
