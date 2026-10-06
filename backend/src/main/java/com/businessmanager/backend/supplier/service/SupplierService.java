package com.businessmanager.backend.supplier.service;

import com.businessmanager.backend.supplier.dto.SupplierCreateDto;
import com.businessmanager.backend.supplier.dto.SupplierResponseDto;
import com.businessmanager.backend.supplier.dto.SupplierUpdateDto;
import com.businessmanager.backend.supplier.dto.SupplierSummaryDto;
import com.businessmanager.backend.supplier.enums.SupplierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {

    /**
     * Create a new supplier. Checks for duplicate supplier_code.
     */
    SupplierResponseDto createSupplier(SupplierCreateDto dto);

    /**
     * Retrieve a supplier by ID.
     */
    SupplierResponseDto getSupplierById(Long id);

    /**
     * Retrieve a supplier by supplierCode.
     */
    SupplierResponseDto getSupplierByCode(String code);

    /**
     * Search and filter suppliers with pagination.
     */
    Page<SupplierSummaryDto> searchSuppliers(
            String name,
            String code,
            String phone,
            SupplierStatus status,
            Pageable pageable
    );

    /**
     * Update an existing supplier profile.
     */
    SupplierResponseDto updateSupplier(Long id, SupplierUpdateDto dto);

    /**
     * Deactivate a supplier (soft-delete).
     */
    void deactivateSupplier(Long id);

    /**
     * Hard-delete a supplier if they have no transaction history.
     */
    void deleteSupplier(Long id);

    /**
     * Reconcile stored supplier balance with transaction history to check for drift.
     */
    com.businessmanager.backend.supplier.dto.SupplierReconciliationDto reconcileBalance(Long id);

    /**
     * Adjust the stored supplier running balance by a net transaction amount.
     * Positive for purchase invoices, negative for payments/debit notes.
     */
    void adjustBalance(Long id, java.math.BigDecimal amount);

    /**
     * Generate supplier statement of account for a date range.
     */
    com.businessmanager.backend.supplier.dto.SupplierStatementResponse getStatement(
            Long supplierId, 
            java.time.LocalDate startDate, 
            java.time.LocalDate endDate
    );
}
