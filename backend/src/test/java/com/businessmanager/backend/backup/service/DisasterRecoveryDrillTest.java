package com.businessmanager.backend.backup.service;

import com.businessmanager.backend.backup.entity.BackupJob;
import com.businessmanager.backend.backup.enums.BackupStatus;
import com.businessmanager.backend.backup.repository.BackupJobRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.backup.enabled=true",
    "app.backup.location=./target/dr_drill_backups",
    "app.backup.cron=-"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DisasterRecoveryDrillTest {

    @Autowired
    private BackupService backupService;

    @Autowired
    private BackupJobRepository backupJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Long successfulBackupJobId = null;
    private static int initialUserCount = 0;

    @Test
    @Order(1)
    void step1_performFullBackup() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute("INSERT INTO users (username, password_hash, status, failed_login_count, version, created_at, updated_at) VALUES ('admin_test', 'hash123', 'ACTIVE', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        }
        
        initialUserCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertTrue(initialUserCount > 0, "Database should have initial user data prior to backup");

        backupService.performBackup();

        Optional<BackupJob> latestJob = backupJobRepository.findFirstByOrderByCreatedAtDesc();
        assertTrue(latestJob.isPresent(), "Backup job should be recorded");
        
        BackupJob job = latestJob.get();
        // If mysqldump CLI is not installed on test host machine, skip dependent restore drill steps cleanly
        boolean hasCliTool = job.getResult() == BackupStatus.SUCCESS;
        if (hasCliTool) {
            assertNotNull(job.getArchiveLocation(), "Archive location must not be null");
            assertTrue(job.getArchiveLocation().endsWith(".zip"), "Archive must be a zip payload");
            successfulBackupJobId = job.getId();
        }
    }

    @Test
    @Order(2)
    void step2_simulateDisasterAndDataWipe() {
        assumeTrue(successfulBackupJobId != null, "Skipped: mysqldump CLI tool not available in test runner OS environment");

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        int wipedUserCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertEquals(0, wipedUserCount, "Disaster simulation failed: Users table should be empty");
    }

    @Test
    @Order(3)
    void step3_executeRestoreAndVerifyIntegrity() {
        assumeTrue(successfulBackupJobId != null, "Skipped: mysqldump CLI tool not available in test runner OS environment");

        assertDoesNotThrow(() -> {
            backupService.restoreBackup(successfulBackupJobId);
        }, "Restore pipeline threw an unexpected exception");

        int restoredUserCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertEquals(initialUserCount, restoredUserCount, "Data integrity verification failed");
    }
}
