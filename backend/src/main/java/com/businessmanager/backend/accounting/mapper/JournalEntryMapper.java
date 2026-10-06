package com.businessmanager.backend.accounting.mapper;

import com.businessmanager.backend.accounting.dto.*;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.entity.JournalLine;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JournalEntryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fiscalPeriod", ignore = true)
    @Mapping(target = "lines", source = "lines")
    JournalEntry toEntity(JournalEntryCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "journalEntry", ignore = true)
    @Mapping(target = "account.id", source = "accountId")
    JournalLine toEntity(JournalLineRequest dto);

    List<JournalLine> toEntityList(List<JournalLineRequest> dtos);

    @Mapping(target = "fiscalPeriodId", source = "fiscalPeriod.id")
    @Mapping(target = "fiscalPeriodName", source = "fiscalPeriod.name")
    @Mapping(target = "totalDebit", ignore = true)
    @Mapping(target = "totalCredit", ignore = true)
    JournalEntryResponseDto toResponseDto(JournalEntry entity);

    List<JournalEntryResponseDto> toResponseDtoList(List<JournalEntry> entities);

    @Mapping(target = "totalAmount", ignore = true)
    JournalEntrySummaryDto toSummaryDto(JournalEntry entity);

    List<JournalEntrySummaryDto> toSummaryDtoList(List<JournalEntry> entities);

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountCode", source = "account.code")
    @Mapping(target = "accountName", source = "account.name")
    JournalLineResponseDto toResponseDto(JournalLine entity);

    List<JournalLineResponseDto> toLineResponseDtoList(List<JournalLine> entities);

    @AfterMapping
    default void calculateTotals(JournalEntry entity, @MappingTarget JournalEntryResponseDto dto) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        if (entity.getLines() != null) {
            for (JournalLine line : entity.getLines()) {
                totalDebit = totalDebit.add(line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO);
                totalCredit = totalCredit.add(line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO);
            }
        }
        dto.setTotalDebit(totalDebit);
        dto.setTotalCredit(totalCredit);
    }

    @AfterMapping
    default void calculateSummaryTotal(JournalEntry entity, @MappingTarget JournalEntrySummaryDto dto) {
        BigDecimal total = BigDecimal.ZERO;
        if (entity.getLines() != null) {
            for (JournalLine line : entity.getLines()) {
                total = total.add(line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO);
            }
        }
        dto.setTotalAmount(total);
    }
}
