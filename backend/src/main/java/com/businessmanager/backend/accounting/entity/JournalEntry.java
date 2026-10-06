package com.businessmanager.backend.accounting.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
public class JournalEntry extends BaseEntity {

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "reference", nullable = false, length = 100)
    private String reference;

    @Column(name = "memo", length = 255)
    private String memo;

    @Column(name = "source_module", length = 50)
    private String sourceModule;

    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id")
    private FiscalPeriod fiscalPeriod;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalLine> lines = new ArrayList<>();

    public void addLine(JournalLine line) {
        lines.add(line);
        line.setJournalEntry(this);
    }

    // Bridging methods for backward compatibility with existing legacy references
    public String getReferenceType() {
        return this.sourceModule;
    }

    public void setReferenceType(String referenceType) {
        this.sourceModule = referenceType;
    }

    public Long getReferenceId() {
        return this.sourceDocumentId;
    }

    public void setReferenceId(Long referenceId) {
        this.sourceDocumentId = referenceId;
    }

    public String getReferenceNumber() {
        return this.reference;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.reference = referenceNumber;
    }

    public String getDescription() {
        return this.memo;
    }

    public void setDescription(String description) {
        this.memo = description;
    }
}
