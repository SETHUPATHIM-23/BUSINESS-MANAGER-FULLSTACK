package com.businessmanager.backend.reports.controller;

import com.businessmanager.backend.reports.dto.ScheduledReportDto;
import com.businessmanager.backend.reports.entity.ReportDefinition;
import com.businessmanager.backend.reports.entity.ScheduledReport;
import com.businessmanager.backend.reports.enums.ReportRunStatus;
import com.businessmanager.backend.reports.repository.ReportDefinitionRepository;
import com.businessmanager.backend.reports.repository.ScheduledReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports/scheduled")
@RequiredArgsConstructor
public class ScheduledReportController {

    private final ScheduledReportRepository scheduledReportRepository;
    private final ReportDefinitionRepository reportDefinitionRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('REPORTING_READ')")
    public ResponseEntity<List<ScheduledReportDto>> getAllScheduledReports() {
        List<ScheduledReportDto> list = scheduledReportRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REPORTING_WRITE')")
    public ResponseEntity<ScheduledReportDto> createScheduledReport(@RequestBody ScheduledReportDto dto) {
        ReportDefinition def = new ReportDefinition();
        def.setName(dto.getName());
        def.setDescription(dto.getDescription());
        def.setReportType(dto.getReportType());
        def.setFilterConfigJson(dto.getFilterConfigJson());
        def = reportDefinitionRepository.save(def);

        ScheduledReport sr = new ScheduledReport();
        sr.setReportDefinition(def);
        sr.setRecurrenceRule(dto.getRecurrenceRule());
        sr.setCronExpression(dto.getCustomCronExpression());
        sr.setPrintTarget(dto.getPrintTarget());
        sr.setLastRunStatus(ReportRunStatus.PENDING);
        sr.setNextRunTime(LocalDateTime.now().plusHours(1)); // Initial schedule
        
        sr = scheduledReportRepository.save(sr);
        return ResponseEntity.ok(toDto(sr));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REPORTING_WRITE')")
    public ResponseEntity<Void> deleteScheduledReport(@PathVariable Long id) {
        scheduledReportRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private ScheduledReportDto toDto(ScheduledReport sr) {
        ScheduledReportDto dto = new ScheduledReportDto();
        dto.setId(sr.getId());
        dto.setName(sr.getReportDefinition().getName());
        dto.setDescription(sr.getReportDefinition().getDescription());
        dto.setReportType(sr.getReportDefinition().getReportType());
        dto.setFilterConfigJson(sr.getReportDefinition().getFilterConfigJson());
        dto.setRecurrenceRule(sr.getRecurrenceRule());
        dto.setCustomCronExpression(sr.getCronExpression());
        dto.setPrintTarget(sr.getPrintTarget());
        dto.setLastRunStatus(sr.getLastRunStatus());
        dto.setLastRunTime(sr.getLastRunTime());
        dto.setNextRunTime(sr.getNextRunTime());
        return dto;
    }
}
