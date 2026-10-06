package com.businessmanager.backend.employee.controller;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.employee.dto.*;
import com.businessmanager.backend.employee.entity.Employee;
import com.businessmanager.backend.employee.entity.AttendanceRecord;
import com.businessmanager.backend.employee.enums.EmployeeStatus;
import com.businessmanager.backend.employee.enums.AttendanceStatus;
import com.businessmanager.backend.employee.mapper.*;
import com.businessmanager.backend.employee.service.*;
import com.businessmanager.backend.location.entity.Location;
import com.businessmanager.backend.location.repository.LocationRepository;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AttendanceRecordService attendanceRecordService;
    
    private final LocationRepository locationRepository;
    private final AccountRepository accountRepository;

    private final EmployeeWorkService employeeWorkService;

    private final EmployeeMapper employeeMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final EmployeeWorkMapper employeeWorkMapper;

    // ── EMPLOYEES CRUD ────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<EmployeeResponseDto> createEmployee(@Valid @RequestBody EmployeeCreateRequest dto) {
        Employee employee = employeeMapper.toEntity(dto);
        resolveRelations(dto.getLocationId(), employee);
        
        Employee created = employeeService.createEmployee(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeMapper.toResponseDto(created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<Page<EmployeeSummaryDto>> searchEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "employeeCode,asc") String sort) {

        EmployeeStatus employeeStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            employeeStatus = EmployeeStatus.valueOf(status.toUpperCase());
        }

        // Parse sorting
        String[] sortParts = sort.split(",");
        Sort sortOrder = Sort.by(sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC, sortParts[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Employee> employeePage = employeeService.searchEmployees(name, code, department, employeeStatus, locationId, pageable);

        List<EmployeeSummaryDto> summaryList = employeeMapper.toSummaryDtoList(employeePage.getContent());
        return ResponseEntity.ok(new PageImpl<>(summaryList, pageable, employeePage.getTotalElements()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(@PathVariable Long id) {
        Employee employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employeeMapper.toResponseDto(employee));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<EmployeeResponseDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest dto) {
        Employee employeeDetails = employeeMapper.toEntity(dto);
        resolveRelations(dto.getLocationId(), employeeDetails);

        Employee updated = employeeService.updateEmployee(id, employeeDetails);
        return ResponseEntity.ok(employeeMapper.toResponseDto(updated));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<EmployeeResponseDto> deactivateEmployee(@PathVariable Long id) {
        Employee deactivated = employeeService.deactivateEmployee(id);
        return ResponseEntity.ok(employeeMapper.toResponseDto(deactivated));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<EmployeeResponseDto> activateEmployee(@PathVariable Long id) {
        Employee activated = employeeService.activateEmployee(id);
        return ResponseEntity.ok(employeeMapper.toResponseDto(activated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }



    // ── ATTENDANCE LOGGING ────────────────────────────────────────────────

    @PostMapping("/attendance")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<AttendanceRecordResponseDto> logAttendance(@Valid @RequestBody AttendanceRecordRequest dto) {
        AttendanceRecord record = attendanceRecordMapper.toEntity(dto);
        
        // Resolve Employee relation
        Employee employee = employeeService.getEmployeeById(dto.getEmployeeId());
        record.setEmployee(employee);

        if (dto.getStatus() != null) {
            record.setStatus(AttendanceStatus.valueOf(dto.getStatus().toUpperCase()));
        }

        AttendanceRecord logged = attendanceRecordService.logAttendance(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceRecordMapper.toResponseDto(logged));
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<Page<AttendanceRecordResponseDto>> searchAttendance(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date,desc") String sort) {

        AttendanceStatus attendanceStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            attendanceStatus = AttendanceStatus.valueOf(status.toUpperCase());
        }

        String[] sortParts = sort.split(",");
        Sort sortOrder = Sort.by(sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC, sortParts[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<AttendanceRecord> attendancePage = attendanceRecordService.searchAttendance(employeeId, startDate, endDate, attendanceStatus, pageable);

        List<AttendanceRecordResponseDto> dtoList = attendanceRecordMapper.toResponseDtoList(attendancePage.getContent());
        return ResponseEntity.ok(new PageImpl<>(dtoList, pageable, attendancePage.getTotalElements()));
    }

    @GetMapping("/{id}/attendance-history")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<List<AttendanceRecordResponseDto>> getEmployeeAttendanceHistory(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<AttendanceRecord> list = attendanceRecordService.getEmployeeAttendanceHistory(id, startDate, endDate);
        return ResponseEntity.ok(attendanceRecordMapper.toResponseDtoList(list));
    }

    @PutMapping("/attendance/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<AttendanceRecordResponseDto> updateAttendance(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceRecordRequest dto) {
        AttendanceRecord record = attendanceRecordMapper.toEntity(dto);
        if (dto.getEmployeeId() != null) {
            Employee employee = employeeService.getEmployeeById(dto.getEmployeeId());
            record.setEmployee(employee);
        }
        if (dto.getStatus() != null) {
            record.setStatus(AttendanceStatus.valueOf(dto.getStatus().toUpperCase()));
        }
        AttendanceRecord updated = attendanceRecordService.updateAttendance(id, record);
        return ResponseEntity.ok(attendanceRecordMapper.toResponseDto(updated));
    }

    @DeleteMapping("/attendance/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        attendanceRecordService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }

    // ── DAILY WORK ENTRIES ────────────────────────────────────────────────

    @PostMapping("/work-entries")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<EmployeeWorkEntryResponseDto> logWorkEntry(@Valid @RequestBody EmployeeWorkEntryRequest dto) {
        var entity = employeeWorkMapper.toEntity(dto);
        var created = employeeWorkService.logWorkEntry(entity, dto.getEmployeeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeWorkMapper.toResponseDto(created));
    }

    @GetMapping("/work-entries")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<Page<EmployeeWorkEntryResponseDto>> searchWorkEntries(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort sortOrder = Sort.by(sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC, sortParts[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        var pageResult = employeeWorkService.searchWorkEntries(employeeId, startDate, endDate, pageable);
        List<EmployeeWorkEntryResponseDto> dtoList = employeeWorkMapper.toWorkEntryResponseDtoList(pageResult.getContent());
        return ResponseEntity.ok(new PageImpl<>(dtoList, pageable, pageResult.getTotalElements()));
    }

    @PutMapping("/work-entries/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<EmployeeWorkEntryResponseDto> updateWorkEntry(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeWorkEntryRequest dto) {
        var entity = employeeWorkMapper.toEntity(dto);
        var updated = employeeWorkService.updateWorkEntry(id, entity);
        return ResponseEntity.ok(employeeWorkMapper.toResponseDto(updated));
    }

    @DeleteMapping("/work-entries/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<Void> deleteWorkEntry(@PathVariable Long id) {
        employeeWorkService.deleteWorkEntry(id);
        return ResponseEntity.noContent().build();
    }

    // ── EMPLOYEE SETTLEMENTS ──────────────────────────────────────────────

    @PostMapping("/settlements")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public ResponseEntity<EmployeeSettlementResponseDto> createSettlement(@Valid @RequestBody EmployeeSettlementRequest dto) {
        var entity = employeeWorkMapper.toEntity(dto);
        var created = employeeWorkService.createSettlement(entity, dto.getEmployeeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeWorkMapper.toResponseDto(created));
    }

    @GetMapping("/settlements")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<Page<EmployeeSettlementResponseDto>> searchSettlements(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "settlementDate,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort sortOrder = Sort.by(sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC, sortParts[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        var pageResult = employeeWorkService.searchSettlements(employeeId, startDate, endDate, pageable);
        List<EmployeeSettlementResponseDto> dtoList = employeeWorkMapper.toSettlementResponseDtoList(pageResult.getContent());
        return ResponseEntity.ok(new PageImpl<>(dtoList, pageable, pageResult.getTotalElements()));
    }

    // ── EMPLOYEE BALANCES ────────────────────────────────────────────────

    @GetMapping("/balances")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<List<EmployeeBalanceSummaryDto>> getAllEmployeeBalances() {
        return ResponseEntity.ok(employeeWorkService.getAllEmployeeBalances());
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<EmployeeBalanceSummaryDto> getEmployeeBalance(@PathVariable Long id) {
        return ResponseEntity.ok(employeeWorkService.getEmployeeBalance(id));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    private void resolveRelations(Long locationId, Employee employee) {
        if (locationId != null) {
            Location location = locationRepository.findById(locationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Location not found with ID: " + locationId));
            employee.setLocation(location);
        }
    }
}
