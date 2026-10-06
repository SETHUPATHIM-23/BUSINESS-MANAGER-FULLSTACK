package com.businessmanager.backend.security.service;

import com.businessmanager.backend.security.entity.AuditLogEntry;
import com.businessmanager.backend.security.repository.AuditLogEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogEntryRepository auditLogEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogEntry> searchAuditLogs(String username, String moduleName, String actionType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogEntryRepository.searchAuditLogs(username, moduleName, actionType, startDate, endDate, pageable);
    }
}
