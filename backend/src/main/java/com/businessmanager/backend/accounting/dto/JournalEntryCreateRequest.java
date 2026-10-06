package com.businessmanager.backend.accounting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class JournalEntryCreateRequest {

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;

    @NotBlank(message = "Reference is required")
    @Size(max = 100, message = "Reference cannot exceed 100 characters")
    private String reference;

    @Size(max = 255, message = "Memo cannot exceed 255 characters")
    private String memo;

    @Size(max = 50, message = "Source module cannot exceed 50 characters")
    private String sourceModule;

    private Long sourceDocumentId;

    @NotEmpty(message = "Journal entry must contain at least one line")
    @Valid
    private List<JournalLineRequest> lines;
}
