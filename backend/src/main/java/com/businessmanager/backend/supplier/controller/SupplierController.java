package com.businessmanager.backend.supplier.controller;

import com.businessmanager.backend.supplier.dto.SupplierCreateDto;
import com.businessmanager.backend.supplier.dto.SupplierResponseDto;
import com.businessmanager.backend.supplier.dto.SupplierUpdateDto;
import com.businessmanager.backend.supplier.dto.SupplierSummaryDto;
import com.businessmanager.backend.supplier.dto.SupplierReconciliationDto;
import com.businessmanager.backend.supplier.enums.SupplierStatus;
import com.businessmanager.backend.supplier.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Validated
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_WRITE')")
    public ResponseEntity<SupplierResponseDto> createSupplier(@Valid @RequestBody SupplierCreateDto dto) {
        SupplierResponseDto response = supplierService.createSupplier(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    public ResponseEntity<SupplierResponseDto> getSupplierById(@PathVariable Long id) {
        SupplierResponseDto response = supplierService.getSupplierById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    public ResponseEntity<SupplierResponseDto> getSupplierByCode(@PathVariable String code) {
        SupplierResponseDto response = supplierService.getSupplierByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    public ResponseEntity<Page<SupplierSummaryDto>> searchSuppliers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) SupplierStatus status,
            Pageable pageable
    ) {
        Page<SupplierSummaryDto> response = supplierService.searchSuppliers(name, code, phone, status, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_WRITE')")
    public ResponseEntity<SupplierResponseDto> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierUpdateDto dto
    ) {
        SupplierResponseDto response = supplierService.updateSupplier(id, dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('SUPPLIER_WRITE')")
    public ResponseEntity<Void> deactivateSupplier(@PathVariable Long id) {
        supplierService.deactivateSupplier(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_WRITE')")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/reconcile")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    public ResponseEntity<SupplierReconciliationDto> reconcileBalance(@PathVariable Long id) {
        SupplierReconciliationDto response = supplierService.reconcileBalance(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/adjust-balance")
    @PreAuthorize("hasAuthority('SUPPLIER_WRITE')")
    public ResponseEntity<Void> adjustBalance(
            @PathVariable Long id,
            @RequestParam BigDecimal amount
    ) {
        supplierService.adjustBalance(id, amount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/statement")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    public ResponseEntity<com.businessmanager.backend.supplier.dto.SupplierStatementResponse> getStatement(
            @PathVariable Long id,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate) {
        com.businessmanager.backend.supplier.dto.SupplierStatementResponse statement = supplierService.getStatement(id, startDate, endDate);
        return ResponseEntity.ok(statement);
    }
}
