package com.businessmanager.backend.employee.service;

import com.businessmanager.backend.employee.dto.EmployeeBalanceSummaryDto;
import com.businessmanager.backend.employee.entity.EmployeeSettlement;
import com.businessmanager.backend.employee.entity.EmployeeWorkEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeWorkService {

    EmployeeWorkEntry logWorkEntry(EmployeeWorkEntry entry, Long employeeId);

    EmployeeWorkEntry updateWorkEntry(Long id, EmployeeWorkEntry entryDetails);

    void deleteWorkEntry(Long id);

    Page<EmployeeWorkEntry> searchWorkEntries(Long employeeId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    EmployeeSettlement createSettlement(EmployeeSettlement settlement, Long employeeId);

    Page<EmployeeSettlement> searchSettlements(Long employeeId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    EmployeeBalanceSummaryDto getEmployeeBalance(Long employeeId);

    List<EmployeeBalanceSummaryDto> getAllEmployeeBalances();
}
