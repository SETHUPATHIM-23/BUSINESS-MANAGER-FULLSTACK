package com.businessmanager.backend.truck.controller;

import com.businessmanager.backend.employee.entity.Employee;
import com.businessmanager.backend.employee.service.EmployeeService;
import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.service.InvoiceService;
import com.businessmanager.backend.truck.dto.*;
import com.businessmanager.backend.truck.entity.DeliveryAssignment;
import com.businessmanager.backend.truck.entity.MaintenanceLog;
import com.businessmanager.backend.truck.entity.Truck;
import com.businessmanager.backend.truck.enums.DeliveryStatus;
import com.businessmanager.backend.truck.mapper.DeliveryAssignmentMapper;
import com.businessmanager.backend.truck.mapper.MaintenanceLogMapper;
import com.businessmanager.backend.truck.mapper.TruckMapper;
import com.businessmanager.backend.truck.service.DeliveryAssignmentService;
import com.businessmanager.backend.truck.service.MaintenanceLogService;
import com.businessmanager.backend.truck.service.TruckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trucks")
@RequiredArgsConstructor
public class TruckController {

    private final TruckService truckService;
    private final DeliveryAssignmentService deliveryAssignmentService;
    private final MaintenanceLogService maintenanceLogService;
    private final EmployeeService employeeService;
    private final InvoiceService invoiceService;

    private final TruckMapper truckMapper;
    private final DeliveryAssignmentMapper deliveryAssignmentMapper;
    private final MaintenanceLogMapper maintenanceLogMapper;

    // ── TRUCK CRUD ENDPOINTS ──────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('TRUCK_WRITE')")
    public ResponseEntity<TruckResponseDto> createTruck(@Valid @RequestBody TruckCreateRequest dto) {
        Truck entity = truckMapper.toEntity(dto);
        if (dto.getDriverEmployeeId() != null) {
            Employee driver = employeeService.getEmployeeById(dto.getDriverEmployeeId());
            entity.setDriver(driver);
        }

        Truck created = truckService.createTruck(entity);
        TruckResponseDto response = truckMapper.toResponseDto(created);
        response.setMaintenanceDue(truckService.isMaintenanceDue(created.getId(), null));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TRUCK_READ')")
    public ResponseEntity<Page<TruckResponseDto>> searchTrucks(
            @RequestParam(required = false) String registrationNumber,
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Long driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "registrationNumber,asc") String sort) {

        String[] sortParts = sort.split(",");
        Sort sortOrder = Sort.by(sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC, sortParts[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Truck> truckPage = truckService.searchTrucks(registrationNumber, make, model, driverId, pageable);

        List<TruckResponseDto> dtoList = truckMapper.toResponseDtoList(truckPage.getContent());
        dtoList.forEach(dto -> dto.setMaintenanceDue(truckService.isMaintenanceDue(dto.getId(), null)));

        return ResponseEntity.ok(new PageImpl<>(dtoList, pageable, truckPage.getTotalElements()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TRUCK_READ')")
    public ResponseEntity<TruckResponseDto> getTruckById(@PathVariable Long id) {
        Truck truck = truckService.getTruckById(id);
        TruckResponseDto response = truckMapper.toResponseDto(truck);
        response.setMaintenanceDue(truckService.isMaintenanceDue(truck.getId(), null));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TRUCK_WRITE')")
    public ResponseEntity<TruckResponseDto> updateTruck(@PathVariable Long id, @Valid @RequestBody TruckUpdateRequest dto) {
        Truck details = truckMapper.toEntity(dto);
        if (dto.getDriverEmployeeId() != null) {
            Employee driver = employeeService.getEmployeeById(dto.getDriverEmployeeId());
            details.setDriver(driver);
        } else {
            details.setDriver(null);
        }

        Truck updated = truckService.updateTruck(id, details);
        TruckResponseDto response = truckMapper.toResponseDto(updated);
        response.setMaintenanceDue(truckService.isMaintenanceDue(updated.getId(), null));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TRUCK_WRITE')")
    public ResponseEntity<Void> deleteTruck(@PathVariable Long id) {
        truckService.deleteTruck(id);
        return ResponseEntity.noContent().build();
    }

    // ── MAINTENANCE DUE & EXPENSE POSTING ACTION ENDPOINTS ───────────────

    @GetMapping("/maintenance-due")
    @PreAuthorize("hasAuthority('TRUCK_READ')")
    public ResponseEntity<List<TruckResponseDto>> getTrucksDueForMaintenance(
            @RequestParam(required = false) Integer intervalDays) {
        List<Truck> trucks = truckService.getTrucksDueForMaintenance(intervalDays);
        List<TruckResponseDto> dtoList = truckMapper.toResponseDtoList(trucks);
        dtoList.forEach(dto -> dto.setMaintenanceDue(true));
        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/post-expense")
    @PreAuthorize("hasAuthority('TRUCK_WRITE')")
    public ResponseEntity<Void> postTruckExpense(@Valid @RequestBody TruckExpensePostingRequest dto) {
        truckService.postTruckExpense(dto);
        return ResponseEntity.ok().build();
    }

    // ── DELIVERY ASSIGNMENTS ENDPOINTS ────────────────────────────────────

    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('TRUCK_WRITE')")
    public ResponseEntity<DeliveryAssignmentResponseDto> createAssignment(@Valid @RequestBody DeliveryAssignmentRequest dto) {
        Truck truck = truckService.getTruckById(dto.getTruckId());

        DeliveryAssignment assignment = deliveryAssignmentMapper.toEntity(dto);
        assignment.setTruck(truck);
        if (dto.getInvoiceId() != null) {
            Invoice invoice = invoiceService.getInvoiceById(dto.getInvoiceId());
            assignment.setInvoice(invoice);
        }
        if (dto.getStatus() != null) {
            assignment.setStatus(DeliveryStatus.valueOf(dto.getStatus().toUpperCase()));
        }

        DeliveryAssignment created = deliveryAssignmentService.createAssignment(assignment);
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryAssignmentMapper.toResponseDto(created));
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('TRUCK_READ')")
    public ResponseEntity<Page<DeliveryAssignmentResponseDto>> searchAssignments(
            @RequestParam(required = false) Long truckId,
            @RequestParam(required = false) Long invoiceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        DeliveryStatus deliveryStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            deliveryStatus = DeliveryStatus.valueOf(status.toUpperCase());
        }

        String[] sortParts = sort.split(",");
        Sort sortOrder = Sort.by(sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC, sortParts[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<DeliveryAssignment> assignmentPage = deliveryAssignmentService.searchAssignments(truckId, invoiceId, deliveryStatus, pageable);

        List<DeliveryAssignmentResponseDto> dtoList = deliveryAssignmentMapper.toResponseDtoList(assignmentPage.getContent());
        return ResponseEntity.ok(new PageImpl<>(dtoList, pageable, assignmentPage.getTotalElements()));
    }

    @PatchMapping("/assignments/{id}/status")
    @PreAuthorize("hasAuthority('TRUCK_WRITE')")
    public ResponseEntity<DeliveryAssignmentResponseDto> updateAssignmentStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        DeliveryStatus deliveryStatus = DeliveryStatus.valueOf(status.toUpperCase());
        DeliveryAssignment updated = deliveryAssignmentService.updateAssignmentStatus(id, deliveryStatus);
        return ResponseEntity.ok(deliveryAssignmentMapper.toResponseDto(updated));
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasAuthority('TRUCK_WRITE')")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        deliveryAssignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }

    // ── MAINTENANCE LOGS ENDPOINTS ────────────────────────────────────────

    @PostMapping("/maintenance-logs")
    @PreAuthorize("hasAuthority('TRUCK_WRITE')")
    public ResponseEntity<MaintenanceLogResponseDto> logMaintenance(@Valid @RequestBody MaintenanceLogRequest dto) {
        Truck truck = truckService.getTruckById(dto.getTruckId());

        MaintenanceLog log = maintenanceLogMapper.toEntity(dto);
        log.setTruck(truck);

        MaintenanceLog created = maintenanceLogService.logMaintenance(log);
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceLogMapper.toResponseDto(created));
    }

    @GetMapping("/maintenance-logs")
    @PreAuthorize("hasAuthority('TRUCK_READ')")
    public ResponseEntity<Page<MaintenanceLogResponseDto>> searchLogs(
            @RequestParam(required = false) Long truckId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort sortOrder = Sort.by(sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC, sortParts[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<MaintenanceLog> logPage = maintenanceLogService.searchLogs(truckId, startDate, endDate, type, pageable);

        List<MaintenanceLogResponseDto> dtoList = maintenanceLogMapper.toResponseDtoList(logPage.getContent());
        return ResponseEntity.ok(new PageImpl<>(dtoList, pageable, logPage.getTotalElements()));
    }

    @DeleteMapping("/maintenance-logs/{id}")
    @PreAuthorize("hasAuthority('TRUCK_WRITE')")
    public ResponseEntity<Void> deleteMaintenanceLog(@PathVariable Long id) {
        maintenanceLogService.deleteLog(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/maintenance-cost")
    @PreAuthorize("hasAuthority('TRUCK_READ')")
    public ResponseEntity<BigDecimal> getTotalMaintenanceCost(@PathVariable Long id) {
        BigDecimal totalCost = maintenanceLogService.getTotalMaintenanceCostByTruckId(id);
        return ResponseEntity.ok(totalCost);
    }
}
