package com.businessmanager.backend.employee.mapper;

import com.businessmanager.backend.employee.dto.AttendanceRecordRequest;
import com.businessmanager.backend.employee.dto.AttendanceRecordResponseDto;
import com.businessmanager.backend.employee.entity.AttendanceRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AttendanceRecordMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "status", ignore = true)
    AttendanceRecord toEntity(AttendanceRecordRequest dto);

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeCode", source = "employee.employeeCode")
    @Mapping(target = "employeeName", source = "employee.name")
    AttendanceRecordResponseDto toResponseDto(AttendanceRecord entity);

    List<AttendanceRecordResponseDto> toResponseDtoList(List<AttendanceRecord> entities);
}
