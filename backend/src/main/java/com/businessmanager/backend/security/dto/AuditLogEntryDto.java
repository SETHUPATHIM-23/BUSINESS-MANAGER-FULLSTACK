package com.businessmanager.backend.security.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogEntryDto {
    private Long id;
    private String username;
    private String actionType;
    private String moduleName;
    private String entityId;
    private String beforeValue;
    private String afterValue;
    private LocalDateTime createdAt;
}
