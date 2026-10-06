package com.businessmanager.backend.accounting.repository;

import com.businessmanager.backend.accounting.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findBySourceModuleAndSourceDocumentId(String sourceModule, Long sourceDocumentId);

    boolean existsBySourceModuleAndSourceDocumentId(String sourceModule, Long sourceDocumentId);

    @Query("SELECT je FROM JournalEntry je WHERE je.sourceModule = :referenceType AND je.sourceDocumentId = :referenceId")
    List<JournalEntry> findByReferenceTypeAndReferenceId(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);

    @Query("SELECT COUNT(je) > 0 FROM JournalEntry je WHERE je.sourceModule = :referenceType AND je.sourceDocumentId = :referenceId")
    boolean existsByReferenceTypeAndReferenceId(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);

    @Query("SELECT je FROM JournalEntry je WHERE " +
           "(:search IS NULL OR LOWER(je.reference) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(je.memo) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:startDate IS NULL OR je.entryDate >= :startDate) " +
           "AND (:endDate IS NULL OR je.entryDate <= :endDate) " +
           "AND (:sourceModule IS NULL OR je.sourceModule = :sourceModule) " +
           "AND (:fiscalPeriodId IS NULL OR je.fiscalPeriod.id = :fiscalPeriodId)")
    Page<JournalEntry> searchJournalEntries(
            @Param("search") String search,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("sourceModule") String sourceModule,
            @Param("fiscalPeriodId") Long fiscalPeriodId,
            Pageable pageable
    );
}
