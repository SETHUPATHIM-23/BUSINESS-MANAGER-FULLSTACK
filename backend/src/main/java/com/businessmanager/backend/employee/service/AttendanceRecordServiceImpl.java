package com.businessmanager.backend.employee.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.employee.entity.AttendanceRecord;
import com.businessmanager.backend.employee.enums.AttendanceStatus;
import com.businessmanager.backend.employee.repository.AttendanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceRecordServiceImpl implements AttendanceRecordService {

    private final AttendanceRecordRepository attendanceRecordRepository;

    @Override
    @Transactional
    @AuditAction(action = "LOG_ATTENDANCE", module = "ATTENDANCE")
    public AttendanceRecord logAttendance(AttendanceRecord record) {
        if (attendanceRecordRepository.existsByEmployeeIdAndDate(record.getEmployee().getId(), record.getDate())) {
            throw new BusinessRuleException("Attendance log already exists for this employee on " + record.getDate());
        }
        return attendanceRecordRepository.save(record);
    }

    @Override
    public AttendanceRecord getAttendanceById(Long id) {
        return attendanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with ID: " + id));
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE_ATTENDANCE", module = "ATTENDANCE")
    public AttendanceRecord updateAttendance(Long id, AttendanceRecord recordDetails) {
        AttendanceRecord existing = getAttendanceById(id);
        
        existing.setStatus(recordDetails.getStatus());
        existing.setCheckIn(recordDetails.getCheckIn());
        existing.setCheckOut(recordDetails.getCheckOut());
        
        return attendanceRecordRepository.save(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE_ATTENDANCE", module = "ATTENDANCE")
    public void deleteAttendance(Long id) {
        AttendanceRecord existing = getAttendanceById(id);
        attendanceRecordRepository.delete(existing);
    }

    @Override
    public Page<AttendanceRecord> searchAttendance(Long employeeId, LocalDate startDate, LocalDate endDate, AttendanceStatus status, Pageable pageable) {
        return attendanceRecordRepository.searchAttendance(employeeId, startDate, endDate, status, pageable);
    }

    @Override
    public List<AttendanceRecord> getAttendanceByDate(LocalDate date) {
        return attendanceRecordRepository.findByDate(date);
    }

    @Override
    public List<AttendanceRecord> getEmployeeAttendanceHistory(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return attendanceRecordRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);
    }

    @Override
    public long getAttendanceCountByStatus(Long employeeId, AttendanceStatus status, LocalDate startDate, LocalDate endDate) {
        return attendanceRecordRepository.countAttendanceByStatus(employeeId, status, startDate, endDate);
    }
}
