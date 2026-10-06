package com.businessmanager.backend.backup.entity;

import com.businessmanager.backend.backup.enums.BackupStatus;
import com.businessmanager.backend.backup.enums.VerificationOutcome;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_jobs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class BackupJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 50)
    private BackupStatus result;

    @Column(name = "archive_location", length = 500)
    private String archiveLocation;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_outcome", nullable = false, length = 50)
    private VerificationOutcome verificationOutcome;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedBy
    @Column(name = "initiated_by", updatable = false)
    private String initiatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
