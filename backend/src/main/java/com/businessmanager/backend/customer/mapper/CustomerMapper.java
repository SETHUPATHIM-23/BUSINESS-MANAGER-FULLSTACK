package com.businessmanager.backend.customer.mapper;

import com.businessmanager.backend.common.mapper.BaseMapper;
import com.businessmanager.backend.customer.dto.CustomerCreateDto;
import com.businessmanager.backend.customer.dto.CustomerResponseDto;
import com.businessmanager.backend.customer.dto.CustomerUpdateDto;
import com.businessmanager.backend.customer.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper extends BaseMapper<CustomerResponseDto, Customer> {
    
    Customer toEntity(CustomerCreateDto dto);
    
    void updateEntityFromDto(CustomerUpdateDto dto, @MappingTarget Customer entity);

    com.businessmanager.backend.customer.dto.CustomerSummaryDto toSummaryDto(Customer entity);

    java.util.List<com.businessmanager.backend.customer.dto.CustomerSummaryDto> toSummaryDtoList(java.util.List<Customer> entities);
}
