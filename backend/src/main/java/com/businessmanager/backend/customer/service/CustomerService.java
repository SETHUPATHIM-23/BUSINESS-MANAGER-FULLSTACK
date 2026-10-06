package com.businessmanager.backend.customer.service;

import com.businessmanager.backend.customer.dto.CustomerCreateDto;
import com.businessmanager.backend.customer.dto.CustomerResponseDto;
import com.businessmanager.backend.customer.dto.CustomerUpdateDto;
import com.businessmanager.backend.customer.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    /**
     * Create a new customer record.
     */
    CustomerResponseDto createCustomer(CustomerCreateDto dto);

    /**
     * Look up a customer record by ID.
     */
    CustomerResponseDto getCustomerById(Long id);

    /**
     * Look up a customer record by customer code.
     */
    CustomerResponseDto getCustomerByCode(String code);

    /**
     * Dynamic paginated search for customer records.
     */
    Page<com.businessmanager.backend.customer.dto.CustomerSummaryDto> searchCustomers(String name, String code, String phone, CustomerStatus status, Pageable pageable);

    /**
     * Update an existing customer record.
     */
    CustomerResponseDto updateCustomer(Long id, CustomerUpdateDto dto);

    /**
     * Deactivate a customer record (set status to INACTIVE).
     */
    void deactivateCustomer(Long id);

    /**
     * Hard-delete a customer record if no transaction history exists.
     */
    void deleteCustomer(Long id);

    /**
     * Validate if an invoice of the given amount can be posted for this customer.
     * Checks credit hold state and credit limits.
     */
    void validateCreditLimit(Long customerId, java.math.BigDecimal invoiceAmount);

    /**
     * Generate customer statement of account for a date range.
     */
    com.businessmanager.backend.customer.dto.CustomerStatementResponse getStatement(
            Long customerId, 
            java.time.LocalDate startDate, 
            java.time.LocalDate endDate
    );
}
