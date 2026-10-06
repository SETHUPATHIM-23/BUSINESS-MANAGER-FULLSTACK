package com.businessmanager.backend.supplier.mapper;

import com.businessmanager.backend.common.mapper.BaseMapper;
import com.businessmanager.backend.supplier.dto.SupplierCreateDto;
import com.businessmanager.backend.supplier.dto.SupplierResponseDto;
import com.businessmanager.backend.supplier.dto.SupplierUpdateDto;
import com.businessmanager.backend.supplier.dto.SupplierSummaryDto;
import com.businessmanager.backend.supplier.entity.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupplierMapper extends BaseMapper<SupplierResponseDto, Supplier> {
    
    Supplier toEntity(SupplierCreateDto dto);
    
    void updateEntityFromDto(SupplierUpdateDto dto, @MappingTarget Supplier entity);

    SupplierSummaryDto toSummaryDto(Supplier entity);

    java.util.List<SupplierSummaryDto> toSummaryDtoList(java.util.List<Supplier> entities);
}
