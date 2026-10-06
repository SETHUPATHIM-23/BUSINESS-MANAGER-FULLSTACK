package com.businessmanager.backend.customer.controller;

import com.businessmanager.backend.common.dto.PageResponse;
import com.businessmanager.backend.customer.dto.CustomerCreateDto;
import com.businessmanager.backend.customer.dto.CustomerResponseDto;
import com.businessmanager.backend.customer.dto.CustomerSummaryDto;
import com.businessmanager.backend.customer.dto.CustomerUpdateDto;
import com.businessmanager.backend.customer.enums.CustomerStatus;
import com.businessmanager.backend.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    public ResponseEntity<CustomerResponseDto> createCustomer(@Valid @RequestBody CustomerCreateDto dto) {
        CustomerResponseDto response = customerService.createCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long id) {
        CustomerResponseDto response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public ResponseEntity<CustomerResponseDto> getCustomerByCode(@PathVariable String code) {
        CustomerResponseDto response = customerService.getCustomerByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public ResponseEntity<PageResponse<CustomerSummaryDto>> searchCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) CustomerStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CustomerSummaryDto> page = customerService.searchCustomers(name, code, phone, status, pageable);
        return ResponseEntity.ok(new PageResponse<>(page));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateDto dto) {
        CustomerResponseDto response = customerService.updateCustomer(id, dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    public ResponseEntity<Void> deactivateCustomer(@PathVariable Long id) {
        customerService.deactivateCustomer(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/validate-credit")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public ResponseEntity<Void> validateCredit(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        customerService.validateCreditLimit(id, amount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/statement")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public ResponseEntity<com.businessmanager.backend.customer.dto.CustomerStatementResponse> getStatement(
            @PathVariable Long id,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate) {
        com.businessmanager.backend.customer.dto.CustomerStatementResponse statement = customerService.getStatement(id, startDate, endDate);
        return ResponseEntity.ok(statement);
    }
}
