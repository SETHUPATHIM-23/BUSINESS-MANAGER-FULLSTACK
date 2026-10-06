package com.businessmanager.backend.customer.controller;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.GlobalExceptionHandler;
import com.businessmanager.backend.customer.dto.CustomerCreateDto;
import com.businessmanager.backend.customer.dto.CustomerResponseDto;
import com.businessmanager.backend.customer.dto.CustomerSummaryDto;
import com.businessmanager.backend.customer.dto.CustomerUpdateDto;
import com.businessmanager.backend.customer.enums.CustomerStatus;
import com.businessmanager.backend.customer.service.CustomerService;
import com.businessmanager.backend.security.config.SecurityConfig;
import com.businessmanager.backend.security.jwt.JwtAuthenticationFilter;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
public class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private CustomerCreateDto createDto;
    private CustomerResponseDto responseDto;

    @BeforeEach
    void setUp() {
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
        responseDto.setCreditLimit(new BigDecimal("1000.00"));
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_WRITE")
    public void createCustomer_Authorized_Success() throws Exception {
        when(customerService.createCustomer(any(CustomerCreateDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerCode").value("CUST-001"))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void createCustomer_UnauthorizedRole_Forbidden() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_WRITE")
    public void createCustomer_ValidationError_BadRequest() throws Exception {
        createDto.setName(""); // Invalid name field

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void getCustomerById_Authorized_Success() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerCode").value("CUST-001"));
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void searchCustomers_Authorized_Success() throws Exception {
        CustomerSummaryDto summaryDto = new CustomerSummaryDto();
        summaryDto.setId(1L);
        summaryDto.setCustomerCode("CUST-001");
        summaryDto.setName("John Doe");
        summaryDto.setStatus(CustomerStatus.ACTIVE);

        when(customerService.searchCustomers(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(summaryDto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/customers")
                        .param("name", "John")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerCode").value("CUST-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void validateCredit_Success() throws Exception {
        doNothing().when(customerService).validateCreditLimit(eq(1L), any(BigDecimal.class));

        mockMvc.perform(post("/api/customers/1/validate-credit")
                        .param("amount", "150.00"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void validateCredit_CreditExceeded_Conflict() throws Exception {
        doThrow(new BusinessRuleException("Credit limit exceeded"))
                .when(customerService).validateCreditLimit(eq(1L), any(BigDecimal.class));

        mockMvc.perform(post("/api/customers/1/validate-credit")
                        .param("amount", "5000.00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Credit limit exceeded"));
    }
}
