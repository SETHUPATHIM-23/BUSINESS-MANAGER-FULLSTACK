package com.businessmanager.backend.backup.service;

import com.businessmanager.backend.backup.dto.BackupJobDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BackupService {
    void performBackup();
    Page<BackupJobDto> getBackupHistory(Pageable pageable);
    String getMostRecentBackupStatus();
    void restoreBackup(Long backupId);
    String getBackupLocation();
    void setBackupLocation(String location);
}
