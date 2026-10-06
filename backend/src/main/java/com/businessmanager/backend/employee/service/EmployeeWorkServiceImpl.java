package com.businessmanager.backend.employee.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.employee.dto.EmployeeBalanceSummaryDto;
import com.businessmanager.backend.employee.entity.Employee;
import com.businessmanager.backend.employee.entity.EmployeeSettlement;
import com.businessmanager.backend.employee.entity.EmployeeWorkEntry;
import com.businessmanager.backend.employee.repository.EmployeeRepository;
import com.businessmanager.backend.employee.repository.EmployeeSettlementRepository;
import com.businessmanager.backend.employee.repository.EmployeeWorkEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeWorkServiceImpl implements EmployeeWorkService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeWorkEntryRepository workEntryRepository;
    private final EmployeeSettlementRepository settlementRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE_WORK_ENTRY", module = "EMPLOYEE")
    public EmployeeWorkEntry logWorkEntry(EmployeeWorkEntry entry, Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));
        
        if (entry.getWorkedAmount() == null || entry.getWorkedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Daily worked amount must be greater than zero.");
        }

        entry.setEmployee(employee);
        if (entry.getDate() == null) {
            entry.setDate(LocalDate.now());
        }
        return workEntryRepository.save(entry);
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE_WORK_ENTRY", module = "EMPLOYEE")
    public EmployeeWorkEntry updateWorkEntry(Long id, EmployeeWorkEntry entryDetails) {
        EmployeeWorkEntry existing = workEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work entry not found with ID: " + id));

        if (entryDetails.getWorkedAmount() != null && entryDetails.getWorkedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Daily worked amount must be greater than zero.");
        }

        if (entryDetails.getDate() != null) {
            existing.setDate(entryDetails.getDate());
        }
        if (entryDetails.getWorkedAmount() != null) {
            existing.setWorkedAmount(entryDetails.getWorkedAmount());
        }
        if (entryDetails.getDescription() != null) {
            existing.setDescription(entryDetails.getDescription());
        }

        return workEntryRepository.save(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE_WORK_ENTRY", module = "EMPLOYEE")
    public void deleteWorkEntry(Long id) {
        EmployeeWorkEntry existing = workEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work entry not found with ID: " + id));
        workEntryRepository.delete(existing);
    }

    @Override
    public Page<EmployeeWorkEntry> searchWorkEntries(Long employeeId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return workEntryRepository.searchWorkEntries(employeeId, startDate, endDate, pageable);
    }

    @Override
    @Transactional
    @AuditAction(action = "CREATE_SETTLEMENT", module = "EMPLOYEE")
    public EmployeeSettlement createSettlement(EmployeeSettlement settlement, Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        if (settlement.getAmount() == null || settlement.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Settlement amount must be greater than zero.");
        }

        settlement.setEmployee(employee);
        if (settlement.getSettlementDate() == null) {
            settlement.setSettlementDate(LocalDate.now());
        }

        // Purely record maintaining — save settlement record without connecting to fund/accounting ledgers
        return settlementRepository.save(settlement);
    }

    @Override
    public Page<EmployeeSettlement> searchSettlements(Long employeeId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return settlementRepository.searchSettlements(employeeId, startDate, endDate, pageable);
    }

    @Override
    public EmployeeBalanceSummaryDto getEmployeeBalance(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        BigDecimal totalWorked = workEntryRepository.sumWorkedAmountByEmployeeId(employeeId);
        BigDecimal totalSettled = settlementRepository.sumSettledAmountByEmployeeId(employeeId);
        BigDecimal netBalance = totalWorked.subtract(totalSettled);

        return EmployeeBalanceSummaryDto.builder()
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .name(employee.getName())
                .department(employee.getDepartment())
                .roleTitle(employee.getRoleTitle())
                .totalWorkedAmount(totalWorked)
                .totalSettledAmount(totalSettled)
                .netOutstandingBalance(netBalance)
                .build();
    }

    @Override
    public List<EmployeeBalanceSummaryDto> getAllEmployeeBalances() {
        List<Employee> employees = employeeRepository.findAll();
        List<EmployeeBalanceSummaryDto> summaries = new ArrayList<>();
        for (Employee emp : employees) {
            summaries.add(getEmployeeBalance(emp.getId()));
        }
        return summaries;
    }
}
