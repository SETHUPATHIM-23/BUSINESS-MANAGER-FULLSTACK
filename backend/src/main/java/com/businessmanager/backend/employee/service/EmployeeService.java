package com.businessmanager.backend.employee.service;

import com.businessmanager.backend.employee.entity.Employee;
import com.businessmanager.backend.employee.enums.EmployeeStatus;
import com.businessmanager.backend.employee.dto.SalaryDisbursementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {
    Employee createEmployee(Employee employee);
    Employee getEmployeeById(Long id);
    Employee getEmployeeByCode(String code);
    Employee updateEmployee(Long id, Employee employeeDetails);
    Employee deactivateEmployee(Long id);
    Employee activateEmployee(Long id);
    void deleteEmployee(Long id);
    Page<Employee> searchEmployees(String name, String code, String department, EmployeeStatus status, Long locationId, Pageable pageable);
}
