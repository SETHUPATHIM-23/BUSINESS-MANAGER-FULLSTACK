package com.businessmanager.backend.accounting.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class JournalEntrySummaryDto {
    private Long id;
    private LocalDate entryDate;
    private String reference;
    private String memo;
    private String sourceModule;
    private Long sourceDocumentId;
    private BigDecimal totalAmount;
}
