package com.businessmanager.backend.accounting.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class JournalEntryResponseDto {
    private Long id;
    private LocalDate entryDate;
    private String reference;
    private String memo;
    private String sourceModule;
    private Long sourceDocumentId;
    private Long fiscalPeriodId;
    private String fiscalPeriodName;
    private List<JournalLineResponseDto> lines;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
}
