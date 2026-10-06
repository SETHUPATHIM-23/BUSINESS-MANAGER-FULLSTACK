package com.businessmanager.backend.employee.mapper;

import com.businessmanager.backend.employee.dto.*;
import com.businessmanager.backend.employee.entity.EmployeeSettlement;
import com.businessmanager.backend.employee.entity.EmployeeWorkEntry;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class EmployeeWorkMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "isSettled", ignore = true)
    public abstract EmployeeWorkEntry toEntity(EmployeeWorkEntryRequest dto);

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeCode", source = "employee.employeeCode")
    @Mapping(target = "employeeName", source = "employee.name")
    public abstract EmployeeWorkEntryResponseDto toResponseDto(EmployeeWorkEntry entity);

    public abstract List<EmployeeWorkEntryResponseDto> toWorkEntryResponseDtoList(List<EmployeeWorkEntry> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "fundAccount", ignore = true)
    public abstract EmployeeSettlement toEntity(EmployeeSettlementRequest dto);

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeCode", source = "employee.employeeCode")
    @Mapping(target = "employeeName", source = "employee.name")
    @Mapping(target = "fundAccountId", source = "fundAccount.id")
    @Mapping(target = "fundAccountName", source = "fundAccount.name")
    public abstract EmployeeSettlementResponseDto toResponseDto(EmployeeSettlement entity);

    public abstract List<EmployeeSettlementResponseDto> toSettlementResponseDtoList(List<EmployeeSettlement> entities);
}
