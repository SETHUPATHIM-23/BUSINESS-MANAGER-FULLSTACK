package com.businessmanager.backend.fund.mapper;

import com.businessmanager.backend.fund.dto.FundTransactionResponseDto;
import com.businessmanager.backend.fund.entity.FundTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FundTransactionMapper {

    @Mapping(source = "fundAccount.id", target = "fundAccountId")
    @Mapping(source = "fundAccount.name", target = "fundAccountName")
    @Mapping(source = "fundAccount.accountNumber", target = "fundAccountNumber")
    @Mapping(source = "targetFundAccount.id", target = "targetFundAccountId")
    @Mapping(source = "targetFundAccount.name", target = "targetFundAccountName")
    FundTransactionResponseDto toDto(FundTransaction entity);
}
