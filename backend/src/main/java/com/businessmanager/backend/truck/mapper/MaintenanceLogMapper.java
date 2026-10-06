package com.businessmanager.backend.truck.mapper;

import com.businessmanager.backend.truck.dto.MaintenanceLogRequest;
import com.businessmanager.backend.truck.dto.MaintenanceLogResponseDto;
import com.businessmanager.backend.truck.entity.MaintenanceLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MaintenanceLogMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "truck", ignore = true)
    MaintenanceLog toEntity(MaintenanceLogRequest dto);

    @Mapping(source = "truck.id", target = "truckId")
    @Mapping(source = "truck.registrationNumber", target = "truckRegistrationNumber")
    MaintenanceLogResponseDto toResponseDto(MaintenanceLog entity);

    List<MaintenanceLogResponseDto> toResponseDtoList(List<MaintenanceLog> entities);
}
