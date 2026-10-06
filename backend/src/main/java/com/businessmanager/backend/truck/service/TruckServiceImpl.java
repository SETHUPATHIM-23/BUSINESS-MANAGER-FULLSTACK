package com.businessmanager.backend.truck.service;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.entity.JournalLine;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.service.JournalEntryService;
import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.DuplicateResourceException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.truck.dto.TruckExpensePostingRequest;
import com.businessmanager.backend.truck.entity.Truck;
import com.businessmanager.backend.truck.repository.DeliveryAssignmentRepository;
import com.businessmanager.backend.truck.repository.MaintenanceLogRepository;
import com.businessmanager.backend.truck.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TruckServiceImpl implements TruckService {

    private final TruckRepository truckRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryService journalEntryService;

    @Override
    @Transactional
    @AuditAction(action = "CREATE", module = "TRUCK")
    public Truck createTruck(Truck truck) {
        if (truckRepository.existsByRegistrationNumber(truck.getRegistrationNumber())) {
            throw new DuplicateResourceException("Truck registration number already exists: " + truck.getRegistrationNumber());
        }
        return truckRepository.save(truck);
    }

    @Override
    public Truck getTruckById(Long id) {
        return truckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found with ID: " + id));
    }

    @Override
    public Truck getTruckByRegistrationNumber(String registrationNumber) {
        return truckRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found with registration number: " + registrationNumber));
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE", module = "TRUCK")
    public Truck updateTruck(Long id, Truck truckDetails) {
        Truck existing = getTruckById(id);

        if (!existing.getRegistrationNumber().equalsIgnoreCase(truckDetails.getRegistrationNumber()) &&
                truckRepository.existsByRegistrationNumber(truckDetails.getRegistrationNumber())) {
            throw new DuplicateResourceException("Truck registration number already exists: " + truckDetails.getRegistrationNumber());
        }

        existing.setRegistrationNumber(truckDetails.getRegistrationNumber());
        existing.setMake(truckDetails.getMake());
        existing.setModel(truckDetails.getModel());
        existing.setCapacity(truckDetails.getCapacity());
        existing.setFuelType(truckDetails.getFuelType());
        existing.setDriver(truckDetails.getDriver());
        existing.setLastServiceDate(truckDetails.getLastServiceDate());

        return truckRepository.save(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "TRUCK")
    public void deleteTruck(Long id) {
        Truck truck = getTruckById(id);

        boolean hasAssignments = deliveryAssignmentRepository.existsByTruckId(id);
        boolean hasLogs = maintenanceLogRepository.existsByTruckId(id);

        if (hasAssignments || hasLogs) {
            throw new BusinessRuleException("Truck has associated delivery assignments or maintenance history and cannot be deleted.");
        }

        truckRepository.delete(truck);
    }

    @Override
    public Page<Truck> searchTrucks(String registrationNumber, String make, String model, Long driverId, Pageable pageable) {
        return truckRepository.searchTrucks(registrationNumber, make, model, driverId, pageable);
    }

    @Override
    public boolean isMaintenanceDue(Long truckId, Integer customIntervalDays) {
        Truck truck = getTruckById(truckId);
        if (truck.getLastServiceDate() == null) {
            return true;
        }

        int intervalDays = (customIntervalDays != null && customIntervalDays > 0) ? customIntervalDays : 90;
        long daysElapsed = ChronoUnit.DAYS.between(truck.getLastServiceDate(), LocalDate.now());
        return daysElapsed >= intervalDays;
    }

    @Override
    public List<Truck> getTrucksDueForMaintenance(Integer customIntervalDays) {
        return truckRepository.findAll().stream()
                .filter(truck -> isMaintenanceDue(truck.getId(), customIntervalDays))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditAction(action = "POST_EXPENSE", module = "TRUCK")
    public void postTruckExpense(TruckExpensePostingRequest request) {
        Truck truck = getTruckById(request.getTruckId());

        Account expenseAccount = accountRepository.findById(request.getExpenseAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense ledger account not found with ID: " + request.getExpenseAccountId()));

        Account paymentAccount = accountRepository.findById(request.getPaymentAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment ledger account not found with ID: " + request.getPaymentAccountId()));

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(request.getDate() != null ? request.getDate() : LocalDate.now());
        entry.setReference("TRUCK-EXP-" + truck.getRegistrationNumber() + "-" + System.currentTimeMillis());
        entry.setMemo("Vehicle Operating Expense (" + request.getExpenseType() + ") for Truck " + truck.getRegistrationNumber() +
                (request.getMemo() != null ? ": " + request.getMemo() : ""));
        entry.setSourceModule("TRUCK_LOGISTICS");
        entry.setSourceDocumentId(truck.getId());

        // Debit Vehicle Operating Expense account
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(expenseAccount);
        debitLine.setDebitAmount(request.getAmount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Vehicle Operating Expense - " + request.getExpenseType());
        entry.addLine(debitLine);

        // Credit Cash/Bank payment account
        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(paymentAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(request.getAmount());
        creditLine.setDescription("Payment for Truck Expense - " + truck.getRegistrationNumber());
        entry.addLine(creditLine);

        journalEntryService.postJournalEntry(entry);
    }
}
