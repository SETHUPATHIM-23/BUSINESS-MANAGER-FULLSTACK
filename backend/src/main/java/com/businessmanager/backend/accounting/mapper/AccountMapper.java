package com.businessmanager.backend.accounting.mapper;

import com.businessmanager.backend.accounting.dto.AccountCreateRequest;
import com.businessmanager.backend.accounting.dto.AccountResponseDto;
import com.businessmanager.backend.accounting.dto.AccountSummaryDto;
import com.businessmanager.backend.accounting.dto.AccountUpdateRequest;
import com.businessmanager.backend.accounting.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    Account toEntity(AccountCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    Account toEntity(AccountUpdateRequest dto);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentCode", source = "parent.code")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "debitTotal", ignore = true)
    @Mapping(target = "creditTotal", ignore = true)
    @Mapping(target = "netBalance", ignore = true)
    AccountResponseDto toResponseDto(Account entity);

    List<AccountResponseDto> toResponseDtoList(List<Account> entities);

    AccountSummaryDto toSummaryDto(Account entity);

    List<AccountSummaryDto> toSummaryDtoList(List<Account> entities);
}
