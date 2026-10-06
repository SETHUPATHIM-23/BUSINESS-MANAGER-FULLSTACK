package com.businessmanager.backend.common.audit.repository;

import com.businessmanager.backend.common.audit.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Immutable repository for Audit Log entries.
 * Does not expose delete or update operations to prevent tampering.
 */
public interface AuditLogRepository extends Repository<AuditLogEntry, Long> {
    
    AuditLogEntry save(AuditLogEntry entity);
    
    Optional<AuditLogEntry> findById(Long id);
    
    Page<AuditLogEntry> findAll(Pageable pageable);
}
