package com.businessmanager.backend.accounting.mapper;

import com.businessmanager.backend.accounting.dto.FiscalPeriodRequest;
import com.businessmanager.backend.accounting.dto.FiscalPeriodResponseDto;
import com.businessmanager.backend.accounting.entity.FiscalPeriod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FiscalPeriodMapper {

    @Mapping(target = "id", ignore = true)
    FiscalPeriod toEntity(FiscalPeriodRequest dto);

    FiscalPeriodResponseDto toResponseDto(FiscalPeriod entity);

    List<FiscalPeriodResponseDto> toResponseDtoList(List<FiscalPeriod> entities);
}
