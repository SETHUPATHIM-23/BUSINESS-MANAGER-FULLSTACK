package com.businessmanager.backend.employee.mapper;

import com.businessmanager.backend.employee.dto.*;
import com.businessmanager.backend.employee.entity.Employee;
import com.businessmanager.backend.security.util.SecurityUtils;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class EmployeeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "status", ignore = true)
    public abstract Employee toEntity(EmployeeCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "status", ignore = true)
    public abstract Employee toEntity(EmployeeUpdateRequest dto);

    @Mapping(target = "locationId", source = "location.id")
    @Mapping(target = "locationCode", source = "location.code")
    @Mapping(target = "locationName", source = "location.name")
    public abstract EmployeeResponseDto toResponseDto(Employee entity);

    @Mapping(target = "locationName", source = "location.name")
    public abstract EmployeeSummaryDto toSummaryDto(Employee entity);

    public abstract List<EmployeeResponseDto> toResponseDtoList(List<Employee> entities);

    public abstract List<EmployeeSummaryDto> toSummaryDtoList(List<Employee> entities);
}
