package com.businessmanager.backend.truck.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.service.JournalEntryService;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.DuplicateResourceException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.truck.dto.TruckExpensePostingRequest;
import com.businessmanager.backend.truck.entity.Truck;
import com.businessmanager.backend.truck.repository.DeliveryAssignmentRepository;
import com.businessmanager.backend.truck.repository.MaintenanceLogRepository;
import com.businessmanager.backend.truck.repository.TruckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TruckServiceImplTest {

    @Mock private TruckRepository truckRepository;
    @Mock private DeliveryAssignmentRepository deliveryAssignmentRepository;
    @Mock private MaintenanceLogRepository maintenanceLogRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private JournalEntryService journalEntryService;

    @InjectMocks
    private TruckServiceImpl truckService;

    private Truck truck;

    @BeforeEach
    void setUp() {
        truck = new Truck();
        truck.setId(1L);
        truck.setRegistrationNumber("TRK-101");
        truck.setMake("Volvo");
        truck.setModel("FH16");
        truck.setCapacity(25.0);
        truck.setFuelType("DIESEL");
        truck.setLastServiceDate(LocalDate.now().minusDays(30));
    }

    // ── CRUD PATH TESTS ──────────────────────────────────────────────────

    @Test
    void createTruck_Success() {
        when(truckRepository.existsByRegistrationNumber("TRK-101")).thenReturn(false);
        when(truckRepository.save(any(Truck.class))).thenReturn(truck);

        Truck created = truckService.createTruck(truck);

        assertNotNull(created);
        assertEquals("TRK-101", created.getRegistrationNumber());
        verify(truckRepository, times(1)).save(truck);
    }

    @Test
    void createTruck_DuplicateRegistrationNumber_ThrowsException() {
        when(truckRepository.existsByRegistrationNumber("TRK-101")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> truckService.createTruck(truck));
        verify(truckRepository, never()).save(any());
    }

    @Test
    void getTruckById_Success() {
        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));

        Truck found = truckService.getTruckById(1L);

        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void getTruckById_NotFound_ThrowsException() {
        when(truckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> truckService.getTruckById(99L));
    }

    @Test
    void updateTruck_Success() {
        Truck updateDetails = new Truck();
        updateDetails.setRegistrationNumber("TRK-101");
        updateDetails.setMake("Volvo");
        updateDetails.setModel("FMX");
        updateDetails.setCapacity(30.0);

        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
        when(truckRepository.save(any(Truck.class))).thenReturn(truck);

        Truck updated = truckService.updateTruck(1L, updateDetails);

        assertNotNull(updated);
        verify(truckRepository, times(1)).save(truck);
    }

    // ── DEACTIVATE / CANNOT DELETE WITH HISTORY TESTS ────────────────────

    @Test
    void deleteTruck_Success() {
        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
        when(deliveryAssignmentRepository.existsByTruckId(1L)).thenReturn(false);
        when(maintenanceLogRepository.existsByTruckId(1L)).thenReturn(false);

        truckService.deleteTruck(1L);

        verify(truckRepository, times(1)).delete(truck);
    }

    @Test
    void deleteTruck_WithDeliveryHistory_ThrowsException() {
        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
        when(deliveryAssignmentRepository.existsByTruckId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> truckService.deleteTruck(1L));
        verify(truckRepository, never()).delete(any());
    }

    @Test
    void deleteTruck_WithMaintenanceHistory_ThrowsException() {
        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
        when(deliveryAssignmentRepository.existsByTruckId(1L)).thenReturn(false);
        when(maintenanceLogRepository.existsByTruckId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> truckService.deleteTruck(1L));
        verify(truckRepository, never()).delete(any());
    }

    // ── MAINTENANCE-DUE FLAGGING TESTS ───────────────────────────────────

    @Test
    void isMaintenanceDue_NullLastServiceDate_ReturnsTrue() {
        truck.setLastServiceDate(null);
        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));

        boolean due = truckService.isMaintenanceDue(1L, 90);

        assertTrue(due);
    }

    @Test
    void isMaintenanceDue_ExceedsInterval_ReturnsTrue() {
        truck.setLastServiceDate(LocalDate.now().minusDays(100));
        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));

        boolean due = truckService.isMaintenanceDue(1L, 90);

        assertTrue(due);
    }

    @Test
    void isMaintenanceDue_WithinInterval_ReturnsFalse() {
        truck.setLastServiceDate(LocalDate.now().minusDays(30));
        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));

        boolean due = truckService.isMaintenanceDue(1L, 90);

        assertFalse(due);
    }

    // ── EXPENSE POSTING TESTS ─────────────────────────────────────────────

    @Test
    void postTruckExpense_Success() {
        TruckExpensePostingRequest request = TruckExpensePostingRequest.builder()
                .truckId(1L)
                .expenseAccountId(10L)
                .paymentAccountId(20L)
                .amount(new BigDecimal("250.50"))
                .date(LocalDate.now())
                .expenseType("FUEL")
                .memo("Refuel at Highway Station")
                .build();

        Account expenseAcc = new Account();
        expenseAcc.setId(10L);
        expenseAcc.setCode("5020");
        expenseAcc.setName("Vehicle Fuel Expense");

        Account paymentAcc = new Account();
        paymentAcc.setId(20L);
        paymentAcc.setCode("1010");
        paymentAcc.setName("Petty Cash");

        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(expenseAcc));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(paymentAcc));

        truckService.postTruckExpense(request);

        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryService, times(1)).postJournalEntry(entryCaptor.capture());

        JournalEntry captured = entryCaptor.getValue();
        assertEquals("TRUCK_LOGISTICS", captured.getSourceModule());
        assertEquals(2, captured.getLines().size());
        assertEquals(new BigDecimal("250.50"), captured.getLines().get(0).getDebitAmount());
        assertEquals(new BigDecimal("250.50"), captured.getLines().get(1).getCreditAmount());
    }

    @Test
    void postTruckExpense_InvalidExpenseAccount_ThrowsException() {
        TruckExpensePostingRequest request = TruckExpensePostingRequest.builder()
                .truckId(1L)
                .expenseAccountId(999L)
                .paymentAccountId(20L)
                .amount(new BigDecimal("250.50"))
                .date(LocalDate.now())
                .expenseType("FUEL")
                .build();

        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> truckService.postTruckExpense(request));
        verify(journalEntryService, never()).postJournalEntry(any());
    }
}
