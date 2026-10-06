package com.businessmanager.backend.security.mapper;

import com.businessmanager.backend.security.dto.AuditLogEntryDto;
import com.businessmanager.backend.security.entity.AuditLogEntry;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogEntryMapper {
    AuditLogEntryDto toDto(AuditLogEntry entry);
}
