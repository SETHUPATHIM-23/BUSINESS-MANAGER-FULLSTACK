package com.businessmanager.backend.supplier.controller;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.GlobalExceptionHandler;
import com.businessmanager.backend.supplier.dto.*;
import com.businessmanager.backend.supplier.enums.SupplierStatus;
import com.businessmanager.backend.supplier.service.SupplierService;
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

@WebMvcTest(SupplierController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
public class SupplierControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupplierService supplierService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private SupplierCreateDto createDto;
    private SupplierResponseDto responseDto;

    @BeforeEach
    void setUp() {
        createDto = new SupplierCreateDto();
        createDto.setSupplierCode("SUPP-100");
        createDto.setName("Acme Invoicing");
        createDto.setEmail("billing@acme.com");
        createDto.setOpeningBalance(BigDecimal.ZERO);

        responseDto = new SupplierResponseDto();
        responseDto.setId(1L);
        responseDto.setSupplierCode("SUPP-100");
        responseDto.setName("Acme Invoicing");
        responseDto.setEmail("billing@acme.com");
        responseDto.setOpeningBalance(BigDecimal.ZERO);
        responseDto.setRunningBalance(BigDecimal.ZERO);
        responseDto.setStatus(SupplierStatus.ACTIVE);
    }

    @Test
    @WithMockUser(authorities = {"SUPPLIER_WRITE"})
    void testCreateSupplier_Success() throws Exception {
        when(supplierService.createSupplier(any(SupplierCreateDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierCode").value("SUPP-100"))
                .andExpect(jsonPath("$.name").value("Acme Invoicing"));
    }

    @Test
    @WithMockUser(authorities = {"SUPPLIER_READ"})
    void testCreateSupplier_ForbiddenForReadRole() throws Exception {
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"SUPPLIER_WRITE"})
    void testCreateSupplier_ValidationFailed() throws Exception {
        createDto.setName(""); // Blank name should fail bean validation
        createDto.setEmail("invalid-email"); // Invalid email should fail

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @WithMockUser(authorities = {"SUPPLIER_READ"})
    void testGetSupplierById_Success() throws Exception {
        when(supplierService.getSupplierById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.supplierCode").value("SUPP-100"));
    }

    @Test
    @WithMockUser(authorities = {"SUPPLIER_READ"})
    void testSearchSuppliers_Success() throws Exception {
        SupplierSummaryDto summaryDto = new SupplierSummaryDto();
        summaryDto.setId(1L);
        summaryDto.setSupplierCode("SUPP-100");
        summaryDto.setName("Acme Invoicing");
        summaryDto.setStatus(SupplierStatus.ACTIVE);

        PageImpl<SupplierSummaryDto> page = new PageImpl<>(Collections.singletonList(summaryDto), PageRequest.of(0, 10), 1);
        when(supplierService.searchSuppliers(eq("Acme"), eq(null), eq(null), eq(null), any())).thenReturn(page);

        mockMvc.perform(get("/api/suppliers")
                        .param("name", "Acme")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].supplierCode").value("SUPP-100"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = {"SUPPLIER_READ"})
    void testReconcileBalance_Success() throws Exception {
        SupplierReconciliationDto reconDto = SupplierReconciliationDto.builder()
                .supplierCode("SUPP-100")
                .supplierName("Acme Invoicing")
                .storedBalance(new BigDecimal("100.00"))
                .recomputedBalance(new BigDecimal("100.00"))
                .drift(BigDecimal.ZERO)
                .reconciled(true)
                .build();

        when(supplierService.reconcileBalance(1L)).thenReturn(reconDto);

        mockMvc.perform(get("/api/suppliers/1/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(true))
                .andExpect(jsonPath("$.drift").value(0));
    }

    @Test
    @WithMockUser(authorities = {"SUPPLIER_WRITE"})
    void testAdjustBalance_Success() throws Exception {
        doNothing().when(supplierService).adjustBalance(eq(1L), any(BigDecimal.class));

        mockMvc.perform(post("/api/suppliers/1/adjust-balance")
                        .param("amount", "250.00"))
                .andExpect(status().isOk());

        verify(supplierService, times(1)).adjustBalance(eq(1L), eq(new BigDecimal("250.00")));
    }
}
