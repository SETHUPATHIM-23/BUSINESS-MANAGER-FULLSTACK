package com.businessmanager.backend.reports.repository;

import com.businessmanager.backend.reports.entity.ScheduledReport;
import com.businessmanager.backend.reports.enums.ReportRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, Long> {

    /**
     * Returns all scheduled reports whose nextRunTime has been reached and whose
     * last run status indicates they are eligible to run again:
     *   - PENDING  : newly created, never yet executed
     *   - FAILED   : previous run failed; allow automatic retry
     *   - SUCCESS  : previous run succeeded and the next window is now open
     * Reports currently being processed (a PENDING set during execution) are
     * intentionally NOT excluded here because the scheduler sets PENDING only
     * inside the processing loop — any true stuck-PENDING record would mean a
     * prior crash and should be retried.
     */
    @Query("SELECT sr FROM ScheduledReport sr " +
           "WHERE sr.nextRunTime <= :now " +
           "AND sr.lastRunStatus IN (:eligibleStatuses)")
    List<ScheduledReport> findDueReports(
            @Param("now") LocalDateTime now,
            @Param("eligibleStatuses") java.util.List<ReportRunStatus> eligibleStatuses
    );
}
