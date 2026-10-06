package com.businessmanager.backend.security.service;

import com.businessmanager.backend.security.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface AuditLogService {
    Page<AuditLogEntry> searchAuditLogs(String username, String moduleName, String actionType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
