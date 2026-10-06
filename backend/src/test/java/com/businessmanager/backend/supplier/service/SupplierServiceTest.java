package com.businessmanager.backend.supplier.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.supplier.dto.*;
import com.businessmanager.backend.supplier.entity.Supplier;
import com.businessmanager.backend.supplier.enums.SupplierStatus;
import com.businessmanager.backend.supplier.mapper.SupplierMapper;
import com.businessmanager.backend.supplier.repository.SupplierRepository;
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
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private Supplier supplier;
    private SupplierCreateDto createDto;
    private SupplierResponseDto responseDto;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setSupplierCode("SUPP-001");
        supplier.setName("Acme Parts");
        supplier.setOpeningBalance(new BigDecimal("100.00"));
        supplier.setRunningBalance(new BigDecimal("100.00"));
        supplier.setStatus(SupplierStatus.ACTIVE);

        createDto = new SupplierCreateDto();
        createDto.setSupplierCode("SUPP-001");
        createDto.setName("Acme Parts");
        createDto.setOpeningBalance(new BigDecimal("100.00"));

        responseDto = new SupplierResponseDto();
        responseDto.setId(1L);
        responseDto.setSupplierCode("SUPP-001");
        responseDto.setName("Acme Parts");
        responseDto.setOpeningBalance(new BigDecimal("100.00"));
        responseDto.setRunningBalance(new BigDecimal("100.00"));
        responseDto.setStatus(SupplierStatus.ACTIVE);
    }

    @Test
    void testCreateSupplier_Success() {
        when(supplierRepository.findBySupplierCode(createDto.getSupplierCode())).thenReturn(Optional.empty());
        when(supplierMapper.toEntity(createDto)).thenReturn(supplier);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);
        when(supplierMapper.toDto(supplier)).thenReturn(responseDto);

        SupplierResponseDto result = supplierService.createSupplier(createDto);

        assertNotNull(result);
        assertEquals(createDto.getSupplierCode(), result.getSupplierCode());
        assertEquals(createDto.getOpeningBalance(), result.getRunningBalance());
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    void testCreateSupplier_DuplicateCodeThrowsException() {
        when(supplierRepository.findBySupplierCode(createDto.getSupplierCode())).thenReturn(Optional.of(supplier));

        assertThrows(BusinessRuleException.class, () -> supplierService.createSupplier(createDto));
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void testGetSupplierById_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierMapper.toDto(supplier)).thenReturn(responseDto);

        SupplierResponseDto result = supplierService.getSupplierById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetSupplierById_NotFoundThrowsException() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplierService.getSupplierById(1L));
    }

    @Test
    void testSearchSuppliers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Supplier> supplierPage = new PageImpl<>(Collections.singletonList(supplier));
        
        when(supplierRepository.searchSuppliers("Acme Parts", "SUPP-001", null, SupplierStatus.ACTIVE, pageable))
                .thenReturn(supplierPage);
        
        SupplierSummaryDto summaryDto = new SupplierSummaryDto();
        summaryDto.setId(1L);
        summaryDto.setSupplierCode("SUPP-001");
        summaryDto.setName("Acme Parts");
        summaryDto.setStatus(SupplierStatus.ACTIVE);
        
        when(supplierMapper.toSummaryDto(supplier)).thenReturn(summaryDto);

        Page<SupplierSummaryDto> result = supplierService.searchSuppliers(
                "Acme Parts", "SUPP-001", null, SupplierStatus.ACTIVE, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("SUPP-001", result.getContent().get(0).getSupplierCode());
    }

    @Test
    void testUpdateSupplier_Success() {
        SupplierUpdateDto updateDto = new SupplierUpdateDto();
        updateDto.setName("Acme Parts Updated");
        updateDto.setStatus(SupplierStatus.ACTIVE);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        doAnswer(invocation -> {
            Supplier s = invocation.getArgument(1);
            s.setName(updateDto.getName());
            return null;
        }).when(supplierMapper).updateEntityFromDto(eq(updateDto), any(Supplier.class));
        
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);
        
        responseDto.setName("Acme Parts Updated");
        when(supplierMapper.toDto(supplier)).thenReturn(responseDto);

        SupplierResponseDto result = supplierService.updateSupplier(1L, updateDto);

        assertNotNull(result);
        assertEquals("Acme Parts Updated", result.getName());
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    void testDeactivateSupplier_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.deactivateSupplier(1L);

        assertEquals(SupplierStatus.INACTIVE, supplier.getStatus());
        verify(supplierRepository, times(1)).save(supplier);
    }

    @Test
    void testDeleteSupplier_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.deleteSupplier(1L);

        verify(supplierRepository, times(1)).delete(supplier);
    }

    @Test
    void testAdjustBalance_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.adjustBalance(1L, new BigDecimal("250.00")); // Increases running balance

        // Initial: 100.00 + 250.00 = 350.00
        assertEquals(new BigDecimal("350.00"), supplier.getRunningBalance());
        verify(supplierRepository, times(1)).save(supplier);
    }

    @Test
    void testReconcileBalance_ReconciledCorrectly() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        // Recomputation sum defaults to 0.00 in mock stub, stored is 100.00, opening is 100.00 -> drift is 0.00
        SupplierReconciliationDto result = supplierService.reconcileBalance(1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getStoredBalance());
        assertEquals(new BigDecimal("100.00"), result.getRecomputedBalance());
        assertEquals(BigDecimal.ZERO, result.getDrift());
        assertTrue(result.isReconciled());
    }

    @Test
    void testReconcileBalance_FlagsDrift() {
        // Force stored running balance to differ from opening balance to test drift tracking
        supplier.setRunningBalance(new BigDecimal("450.00")); 
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        // Recomputation sum is 0.00, recomputed = opening (100.00), stored = 450.00 -> drift = 350.00
        SupplierReconciliationDto result = supplierService.reconcileBalance(1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("450.00"), result.getStoredBalance());
        assertEquals(new BigDecimal("100.00"), result.getRecomputedBalance());
        assertEquals(new BigDecimal("350.00"), result.getDrift());
        assertFalse(result.isReconciled());
    }
}
