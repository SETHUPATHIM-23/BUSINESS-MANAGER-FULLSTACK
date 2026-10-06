package com.businessmanager.backend.fund.mapper;

import com.businessmanager.backend.fund.dto.ReconciliationResponseDto;
import com.businessmanager.backend.fund.entity.ReconciliationRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReconciliationRecordMapper {

    @Mapping(source = "fundTransaction.id", target = "fundTransactionId")
    ReconciliationResponseDto toDto(ReconciliationRecord entity);
}
