package com.businessmanager.backend.customer.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.customer.dto.CustomerCreateDto;
import com.businessmanager.backend.customer.dto.CustomerResponseDto;
import com.businessmanager.backend.customer.dto.CustomerUpdateDto;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.enums.CustomerStatus;
import com.businessmanager.backend.customer.mapper.CustomerMapper;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer customer;
    private CustomerCreateDto createDto;
    private CustomerResponseDto responseDto;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setCustomerCode("CUST-001");
        customer.setName("John Doe");
        customer.setOpeningBalance(BigDecimal.ZERO);
        customer.setCreditLimit(new BigDecimal("1000.00"));
        customer.setCreditHold(false);
        customer.setStatus(CustomerStatus.ACTIVE);

        createDto = new CustomerCreateDto();
        createDto.setCustomerCode("CUST-001");
        createDto.setName("John Doe");
        createDto.setOpeningBalance(BigDecimal.ZERO);
        createDto.setCreditLimit(new BigDecimal("1000.00"));

        responseDto = new CustomerResponseDto();
        responseDto.setId(1L);
        responseDto.setCustomerCode("CUST-001");
        responseDto.setName("John Doe");
        responseDto.setStatus(CustomerStatus.ACTIVE);
    }

    @Test
    void createCustomer_Success() {
        when(customerRepository.findByCustomerCode(createDto.getCustomerCode())).thenReturn(Optional.empty());
        when(customerMapper.toEntity(createDto)).thenReturn(customer);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(customerMapper.toDto(customer)).thenReturn(responseDto);

        CustomerResponseDto result = customerService.createCustomer(createDto);

        assertNotNull(result);
        assertEquals("CUST-001", result.getCustomerCode());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void createCustomer_DuplicateCode_ThrowsBusinessRuleException() {
        when(customerRepository.findByCustomerCode(createDto.getCustomerCode())).thenReturn(Optional.of(customer));

        assertThrows(BusinessRuleException.class, () -> customerService.createCustomer(createDto));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void getCustomerById_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerMapper.toDto(customer)).thenReturn(responseDto);

        CustomerResponseDto result = customerService.getCustomerById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getCustomerById_NotFound_ThrowsResourceNotFoundException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomerById(1L));
    }

    @Test
    void getCustomerByCode_Success() {
        when(customerRepository.findByCustomerCode("CUST-001")).thenReturn(Optional.of(customer));
        when(customerMapper.toDto(customer)).thenReturn(responseDto);

        CustomerResponseDto result = customerService.getCustomerByCode("CUST-001");

        assertNotNull(result);
        assertEquals("CUST-001", result.getCustomerCode());
    }

    @Test
    void updateCustomer_Success() {
        CustomerUpdateDto updateDto = new CustomerUpdateDto();
        updateDto.setName("Jane Doe");
        updateDto.setStatus(CustomerStatus.ACTIVE);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toDto(customer)).thenReturn(responseDto);

        CustomerResponseDto result = customerService.updateCustomer(1L, updateDto);

        assertNotNull(result);
        verify(customerMapper).updateEntityFromDto(updateDto, customer);
        verify(customerRepository).save(customer);
    }

    @Test
    void deactivateCustomer_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.deactivateCustomer(1L);

        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    void deleteCustomer_WithoutHistory_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertDoesNotThrow(() -> customerService.deleteCustomer(1L));
        verify(customerRepository, times(1)).delete(customer);
    }

    @Test
    void validateCreditLimit_CreditHoldActive_ThrowsBusinessRuleException() {
        customer.setCreditHold(true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        BigDecimal invoiceAmount = new BigDecimal("100.00");
        assertThrows(BusinessRuleException.class, () -> customerService.validateCreditLimit(1L, invoiceAmount));
    }

    @Test
    void validateCreditLimit_ExceedsLimit_ThrowsBusinessRuleException() {
        customer.setCreditLimit(new BigDecimal("500.00"));
        customer.setOpeningBalance(new BigDecimal("400.00"));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        BigDecimal invoiceAmount = new BigDecimal("150.00"); // 400 + 150 = 550 > 500
        assertThrows(BusinessRuleException.class, () -> customerService.validateCreditLimit(1L, invoiceAmount));
    }

    @Test
    void validateCreditLimit_WithinLimit_Success() {
        customer.setCreditLimit(new BigDecimal("500.00"));
        customer.setOpeningBalance(new BigDecimal("400.00"));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        BigDecimal invoiceAmount = new BigDecimal("100.00"); // 400 + 100 = 500 <= 500
        assertDoesNotThrow(() -> customerService.validateCreditLimit(1L, invoiceAmount));
    }

    @Test
    void validateCreditLimit_NoLimitSet_Success() {
        customer.setCreditLimit(null);
        customer.setOpeningBalance(new BigDecimal("400.00"));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        BigDecimal invoiceAmount = new BigDecimal("10000.00"); // No limit checks
        assertDoesNotThrow(() -> customerService.validateCreditLimit(1L, invoiceAmount));
    }
}
