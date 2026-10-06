package com.businessmanager.backend.employee.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.employee.entity.Employee;
import com.businessmanager.backend.employee.enums.EmployeeStatus;
import com.businessmanager.backend.employee.repository.EmployeeRepository;
import com.businessmanager.backend.employee.repository.AttendanceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("EMP001");
        employee.setName("John Doe");
        employee.setDepartment("Engineering");
        employee.setRoleTitle("Software Engineer");
        employee.setJoiningDate(LocalDate.of(2026, 1, 1));
        employee.setStatus(EmployeeStatus.ACTIVE);
    }

    @Test
    void createEmployee_Success() {
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        Employee result = employeeService.createEmployee(employee);

        assertNotNull(result);
        assertEquals("EMP001", result.getEmployeeCode());
        verify(employeeRepository).save(employee);
    }

    @Test
    void createEmployee_DuplicateCode_ThrowsException() {
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> employeeService.createEmployee(employee));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void getEmployeeById_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Employee result = employeeService.getEmployeeById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getEmployeeById_NotFound_ThrowsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeById(99L));
    }

    @Test
    void updateEmployee_Success() {
        Employee details = new Employee();
        details.setEmployeeCode("EMP001");
        details.setName("John Smith");
        details.setDepartment("Engineering");
        details.setRoleTitle("Senior Engineer");
        details.setJoiningDate(LocalDate.of(2026, 1, 1));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));

        Employee result = employeeService.updateEmployee(1L, details);

        assertEquals("John Smith", result.getName());
        assertEquals("Senior Engineer", result.getRoleTitle());
        verify(employeeRepository).save(employee);
    }

    @Test
    void updateEmployee_ChangeCodeDuplicate_ThrowsException() {
        Employee details = new Employee();
        details.setEmployeeCode("EMP002");
        details.setName("John Doe");
        details.setDepartment("Engineering");
        details.setRoleTitle("Software Engineer");
        details.setJoiningDate(LocalDate.of(2026, 1, 1));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmployeeCode("EMP002")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> employeeService.updateEmployee(1L, details));
    }

    @Test
    void deactivateEmployee_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        Employee result = employeeService.deactivateEmployee(1L);

        assertEquals(EmployeeStatus.INACTIVE, result.getStatus());
        verify(employeeRepository).save(employee);
    }

    @Test
    void deleteEmployee_NoHistory_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRecordRepository.existsByEmployeeId(1L)).thenReturn(false);

        employeeService.deleteEmployee(1L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    void deleteEmployee_WithHistory_ThrowsException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRecordRepository.existsByEmployeeId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> employeeService.deleteEmployee(1L));
        verify(employeeRepository, never()).delete(any(Employee.class));
    }
}
