package com.businessmanager.backend.employee.service;

import com.businessmanager.backend.employee.entity.AttendanceRecord;
import com.businessmanager.backend.employee.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordService {
    AttendanceRecord logAttendance(AttendanceRecord record);
    AttendanceRecord getAttendanceById(Long id);
    AttendanceRecord updateAttendance(Long id, AttendanceRecord recordDetails);
    void deleteAttendance(Long id);
    Page<AttendanceRecord> searchAttendance(Long employeeId, LocalDate startDate, LocalDate endDate, AttendanceStatus status, Pageable pageable);
    List<AttendanceRecord> getAttendanceByDate(LocalDate date);
    List<AttendanceRecord> getEmployeeAttendanceHistory(Long employeeId, LocalDate startDate, LocalDate endDate);
    long getAttendanceCountByStatus(Long employeeId, AttendanceStatus status, LocalDate startDate, LocalDate endDate);
}
