package com.businessmanager.backend.security.repository;

import com.businessmanager.backend.security.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Read-only audit log repository for admin search and dashboard activity.
 *
 * ADM-040/ADM-080 require audit logs to be append-only and not editable or
 * deletable by any application role. New entries are written through the
 * common audit repository; this projection intentionally exposes no save,
 * delete, or mutating batch methods.
 */
public interface AuditLogEntryRepository extends Repository<AuditLogEntry, Long> {

    Optional<AuditLogEntry> findById(Long id);

    Page<AuditLogEntry> findAll(Pageable pageable);

    @Query("SELECT a FROM AuditLogEntry a WHERE " +
           "(:username IS NULL OR a.username = :username) AND " +
           "(:moduleName IS NULL OR a.moduleName = :moduleName) AND " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate)")
    Page<AuditLogEntry> searchAuditLogs(@Param("username") String username,
                                        @Param("moduleName") String moduleName,
                                        @Param("actionType") String actionType,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);

    @Query("SELECT a FROM AuditLogEntry a WHERE a.moduleName IN :modules ORDER BY a.createdAt DESC")
    Page<AuditLogEntry> findRecentActivitiesByModules(@Param("modules") java.util.List<String> modules, Pageable pageable);
}
