package com.businessmanager.backend.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupJobDto {
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String result;
    private String archiveLocation;
    private Long fileSizeBytes;
    private String verificationOutcome;
    private String errorMessage;
    private String initiatedBy;
    private LocalDateTime createdAt;
}
