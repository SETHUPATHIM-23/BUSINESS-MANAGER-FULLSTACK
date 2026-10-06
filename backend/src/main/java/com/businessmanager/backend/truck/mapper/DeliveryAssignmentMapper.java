package com.businessmanager.backend.truck.mapper;

import com.businessmanager.backend.truck.dto.DeliveryAssignmentRequest;
import com.businessmanager.backend.truck.dto.DeliveryAssignmentResponseDto;
import com.businessmanager.backend.truck.entity.DeliveryAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeliveryAssignmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "truck", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "status", ignore = true)
    DeliveryAssignment toEntity(DeliveryAssignmentRequest dto);

    @Mapping(source = "truck.id", target = "truckId")
    @Mapping(source = "truck.registrationNumber", target = "truckRegistrationNumber")
    @Mapping(source = "invoice.id", target = "invoiceId")
    @Mapping(source = "invoice.invoiceNumber", target = "invoiceNumber")
    @Mapping(source = "invoice.customer.name", target = "customerName")
    DeliveryAssignmentResponseDto toResponseDto(DeliveryAssignment entity);

    List<DeliveryAssignmentResponseDto> toResponseDtoList(List<DeliveryAssignment> entities);
}
