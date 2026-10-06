package com.businessmanager.backend.employee.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.employee.dto.SalaryDisbursementRequest;
import com.businessmanager.backend.employee.entity.Employee;
import com.businessmanager.backend.employee.enums.EmployeeStatus;
import com.businessmanager.backend.employee.repository.EmployeeRepository;
import com.businessmanager.backend.employee.repository.AttendanceRecordRepository;
import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.entity.JournalLine;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.service.JournalEntryService;
import com.businessmanager.backend.common.event.SalaryDisbursementPostedEvent;
import com.businessmanager.backend.fund.repository.FundAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryService journalEntryService;
    private final ApplicationEventPublisher eventPublisher;
    private final FundAccountRepository fundAccountRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE", module = "EMPLOYEE")
    public Employee createEmployee(Employee employee) {
        if (employeeRepository.existsByEmployeeCode(employee.getEmployeeCode())) {
            throw new BusinessRuleException("Employee code '" + employee.getEmployeeCode() + "' is already in use.");
        }
        if (employee.getStatus() == null) {
            employee.setStatus(EmployeeStatus.ACTIVE);
        }
        return employeeRepository.save(employee);
    }

    @Override
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
    }

    @Override
    public Employee getEmployeeByCode(String code) {
        return employeeRepository.findByEmployeeCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with code: " + code));
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE", module = "EMPLOYEE")
    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee existing = getEmployeeById(id);

        if (!existing.getEmployeeCode().equals(employeeDetails.getEmployeeCode()) &&
                employeeRepository.existsByEmployeeCode(employeeDetails.getEmployeeCode())) {
            throw new BusinessRuleException("Employee code '" + employeeDetails.getEmployeeCode() + "' is already in use.");
        }

        existing.setEmployeeCode(employeeDetails.getEmployeeCode());
        existing.setName(employeeDetails.getName());
        existing.setContactDetails(employeeDetails.getContactDetails());
        existing.setDepartment(employeeDetails.getDepartment());
        existing.setRoleTitle(employeeDetails.getRoleTitle());
        existing.setJoiningDate(employeeDetails.getJoiningDate());
        if (employeeDetails.getStatus() != null) {
            existing.setStatus(employeeDetails.getStatus());
        }
        existing.setLocation(employeeDetails.getLocation());

        return employeeRepository.save(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "DEACTIVATE", module = "EMPLOYEE")
    public Employee deactivateEmployee(Long id) {
        Employee existing = getEmployeeById(id);
        existing.setStatus(EmployeeStatus.INACTIVE);
        return employeeRepository.save(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "ACTIVATE", module = "EMPLOYEE")
    public Employee activateEmployee(Long id) {
        Employee existing = getEmployeeById(id);
        existing.setStatus(EmployeeStatus.ACTIVE);
        return employeeRepository.save(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "EMPLOYEE")
    public void deleteEmployee(Long id) {
        Employee existing = getEmployeeById(id);
        if (attendanceRecordRepository.existsByEmployeeId(id)) {
            throw new BusinessRuleException("Employee has associated attendance history and cannot be deleted. Deactivate instead.");
        }
        employeeRepository.delete(existing);
    }

    @Override
    public Page<Employee> searchEmployees(String name, String code, String department, EmployeeStatus status, Long locationId, Pageable pageable) {
        return employeeRepository.searchEmployees(name, code, department, status, locationId, pageable);
    }
}
