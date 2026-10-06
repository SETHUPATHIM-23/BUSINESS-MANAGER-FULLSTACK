package com.businessmanager.backend.fund.mapper;

import com.businessmanager.backend.fund.dto.FundAccountCreateRequest;
import com.businessmanager.backend.fund.dto.FundAccountResponseDto;
import com.businessmanager.backend.fund.entity.FundAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FundAccountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "glAccount", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "active", ignore = true)
    FundAccount toEntity(FundAccountCreateRequest request);

    @Mapping(source = "glAccount.id", target = "glAccountId")
    @Mapping(source = "glAccount.code", target = "glAccountCode")
    @Mapping(source = "glAccount.name", target = "glAccountName")
    @Mapping(source = "location.id", target = "locationId")
    @Mapping(source = "location.name", target = "locationName")
    FundAccountResponseDto toDto(FundAccount entity);
}
