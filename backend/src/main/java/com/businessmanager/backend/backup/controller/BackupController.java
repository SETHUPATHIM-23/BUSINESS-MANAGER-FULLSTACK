package com.businessmanager.backend.backup.controller;

import com.businessmanager.backend.backup.dto.BackupJobDto;
import com.businessmanager.backend.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/backups")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_READ')")
    public ResponseEntity<Page<BackupJobDto>> getBackupHistory(Pageable pageable) {
        return ResponseEntity.ok(backupService.getBackupHistory(pageable));
    }

    @GetMapping("/location")
    @PreAuthorize("hasAuthority('SYSTEM_READ')")
    public ResponseEntity<String> getBackupLocation() {
        return ResponseEntity.ok(backupService.getBackupLocation());
    }

    @PostMapping("/location")
    @PreAuthorize("hasAuthority('SYSTEM_WRITE')")
    public ResponseEntity<String> updateBackupLocation(@RequestBody Map<String, String> body) {
        String path = body.get("path");
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().body("Path cannot be empty");
        }
        backupService.setBackupLocation(path);
        return ResponseEntity.ok(backupService.getBackupLocation());
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasAuthority('SYSTEM_WRITE')")
    public ResponseEntity<String> triggerBackup() {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                backupService.performBackup();
            } catch (Exception ignored) {}
        });
        return ResponseEntity.accepted().body("Backup triggered and running.");
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('SYSTEM_WRITE')")
    public ResponseEntity<String> restoreBackup(@PathVariable Long id) {
        try {
            backupService.restoreBackup(id);
            return ResponseEntity.ok("Backup restored successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Restore failed: " + e.getMessage());
        }
    }
}
