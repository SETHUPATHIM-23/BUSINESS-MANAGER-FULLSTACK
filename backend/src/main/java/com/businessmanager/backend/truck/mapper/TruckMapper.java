package com.businessmanager.backend.truck.mapper;

import com.businessmanager.backend.truck.dto.TruckCreateRequest;
import com.businessmanager.backend.truck.dto.TruckResponseDto;
import com.businessmanager.backend.truck.dto.TruckUpdateRequest;
import com.businessmanager.backend.truck.entity.Truck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TruckMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    Truck toEntity(TruckCreateRequest dto);

    @Mapping(target = "driver", ignore = true)
    Truck toEntity(TruckUpdateRequest dto);

    @Mapping(source = "driver.id", target = "driverEmployeeId")
    @Mapping(source = "driver.name", target = "driverName")
    @Mapping(source = "driver.employeeCode", target = "driverCode")
    @Mapping(target = "maintenanceDue", ignore = true)
    TruckResponseDto toResponseDto(Truck entity);

    List<TruckResponseDto> toResponseDtoList(List<Truck> entities);
}
