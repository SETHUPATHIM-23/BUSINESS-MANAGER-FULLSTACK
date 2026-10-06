package com.businessmanager.backend.backup.service;

import com.businessmanager.backend.backup.dto.BackupJobDto;
import com.businessmanager.backend.backup.entity.BackupJob;
import com.businessmanager.backend.backup.enums.BackupStatus;
import com.businessmanager.backend.backup.enums.VerificationOutcome;
import com.businessmanager.backend.backup.repository.BackupJobRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupServiceImpl implements BackupService {

    private final BackupJobRepository backupJobRepository;
    private final DataSourceProperties dataSourceProperties;
    private final DataSource dataSource;

    @Value("${app.backup.enabled:true}")
    private boolean backupEnabled;

    /** Configurable backup folder — can be set via settings UI */
    @Value("${app.backup.location:./backups}")
    private String backupLocation;

    @Value("${app.backup.mysqldump-path:mysqldump}")
    private String mysqldumpPath;

    @Value("${app.backup.mysql-path:mysql}")
    private String mysqlPath;

    /** Maximum number of backup files to keep. Oldest is deleted when exceeded. */
    @Value("${app.backup.max-keep:5}")
    private int maxKeep;

    @PostConstruct
    public void cleanupStaleInProgressJobs() {
        try {
            List<BackupJob> inProgressJobs = backupJobRepository.findAll().stream()
                    .filter(j -> j.getResult() == BackupStatus.IN_PROGRESS)
                    .toList();
            for (BackupJob job : inProgressJobs) {
                job.setResult(BackupStatus.FAILURE);
                job.setVerificationOutcome(VerificationOutcome.FAILED);
                job.setErrorMessage("Backup timed out or was interrupted.");
                job.setEndTime(LocalDateTime.now());
                backupJobRepository.save(job);
                log.warn("Cleaned up stale IN_PROGRESS backup job ID {}", job.getId());
            }
        } catch (Exception e) {
            log.error("Failed to clean up stale backup jobs", e);
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    @Override
    public Page<BackupJobDto> getBackupHistory(Pageable pageable) {
        // Auto-fail any stuck jobs older than 2 minutes
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
        List<BackupJob> stuck = backupJobRepository.findAll().stream()
                .filter(j -> j.getResult() == BackupStatus.IN_PROGRESS && j.getCreatedAt() != null && j.getCreatedAt().isBefore(threshold))
                .toList();
        for (BackupJob job : stuck) {
            job.setResult(BackupStatus.FAILURE);
            job.setVerificationOutcome(VerificationOutcome.FAILED);
            job.setErrorMessage("Backup operation timed out.");
            job.setEndTime(LocalDateTime.now());
            backupJobRepository.save(job);
        }
        return backupJobRepository.findAll(pageable).map(this::mapToDto);
    }

    @Override
    public String getMostRecentBackupStatus() {
        return backupJobRepository.findFirstByOrderByCreatedAtDesc()
                .map(job -> {
                    String status = job.getResult().name();
                    String time = job.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
                    return status + " (" + time + ")";
                })
                .orElse("No backups found");
    }

    @Override
    public String getBackupLocation() {
        return Paths.get(backupLocation).toAbsolutePath().toString();
    }

    @Override
    public void setBackupLocation(String location) {
        if (location != null && !location.isBlank()) {
            this.backupLocation = location.trim();
            log.info("Backup location updated to: {}", this.backupLocation);
        }
    }

    // ─── Scheduled every 2 days at 02:00 ─────────────────────────────────────

    @Override
    @Scheduled(cron = "${app.backup.cron:0 0 2 */2 * ?}")
    public void performBackup() {
        if (!backupEnabled) {
            log.info("Backup is disabled.");
            return;
        }
        log.info("Starting scheduled database backup…");
        runBackup("SYSTEM_SCHEDULED");
    }

    // ─── Core backup logic ────────────────────────────────────────────────────

    private BackupJob runBackup(String initiatedBy) {
        BackupJob job = new BackupJob();
        job.setStartTime(LocalDateTime.now());
        job.setResult(BackupStatus.IN_PROGRESS);
        job.setVerificationOutcome(VerificationOutcome.PENDING);
        job.setInitiatedBy(initiatedBy);
        job = backupJobRepository.save(job);

        try {
            // Ensure backup folder exists
            Path backupDir = Paths.get(backupLocation);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String dbName = extractDatabaseName(dataSourceProperties.getUrl());
            String sqlFileName = dbName + "_backup_" + timestamp + ".sql";
            String zipFileName = dbName + "_backup_" + timestamp + ".zip";

            Path sqlFilePath = backupDir.resolve(sqlFileName);
            Path zipFilePath = backupDir.resolve(zipFileName);

            // 1. Try mysqldump CLI first, fallback to JDBC exporter if CLI is missing
            try {
                runMysqlDump(sqlFilePath.toString());
            } catch (Exception cliEx) {
                log.warn("mysqldump CLI tool unavailable ({}), generating database dump via JDBC...", cliEx.getMessage());
                exportDatabaseViaJdbc(sqlFilePath.toFile());
            }

            // 2. Compress → .zip
            compressToZip(sqlFilePath.toString(), zipFilePath.toString(), sqlFileName);

            // 3. Delete raw SQL
            Files.deleteIfExists(sqlFilePath);

            // 4. Verify output
            File zipFile = zipFilePath.toFile();
            if (zipFile.exists() && zipFile.length() > 0) {
                job.setResult(BackupStatus.SUCCESS);
                job.setVerificationOutcome(VerificationOutcome.VERIFIED);
                job.setArchiveLocation(zipFilePath.toAbsolutePath().toString());
                job.setFileSizeBytes(zipFile.length());
                log.info("Backup created successfully: {}", zipFilePath.toAbsolutePath());
            } else {
                job.setResult(BackupStatus.FAILURE);
                job.setVerificationOutcome(VerificationOutcome.FAILED);
                job.setErrorMessage("Backup zip was not created or is empty.");
            }

        } catch (Exception e) {
            log.error("Backup failed", e);
            job.setResult(BackupStatus.FAILURE);
            job.setVerificationOutcome(VerificationOutcome.FAILED);
            job.setErrorMessage(e.getMessage());
        } finally {
            job.setEndTime(LocalDateTime.now());
            backupJobRepository.save(job);
        }

        // Prune: keep only the newest `maxKeep` backups
        if (job.getResult() == BackupStatus.SUCCESS) {
            pruneOldBackups();
        }

        return job;
    }

    private void pruneOldBackups() {
        List<BackupJob> all = backupJobRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt"));
        List<BackupJob> successful = all.stream()
                .filter(j -> j.getResult() == BackupStatus.SUCCESS)
                .toList();

        if (successful.size() <= maxKeep) return;

        List<BackupJob> toDelete = successful.subList(maxKeep, successful.size());
        for (BackupJob old : toDelete) {
            try {
                if (old.getArchiveLocation() != null) {
                    Files.deleteIfExists(Paths.get(old.getArchiveLocation()));
                }
                old.setResult(BackupStatus.PURGED);
                old.setArchiveLocation(null);
                old.setErrorMessage("Auto-purged: exceeded max-keep limit of " + maxKeep);
                backupJobRepository.save(old);
                log.info("Pruned old backup ID {}", old.getId());
            } catch (Exception e) {
                log.warn("Could not prune backup ID {}: {}", old.getId(), e.getMessage());
            }
        }
    }

    // ─── Restore ─────────────────────────────────────────────────────────────

    @Override
    public void restoreBackup(Long backupId) {
        BackupJob job = backupJobRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Backup not found: " + backupId));

        if (job.getResult() == BackupStatus.PURGED) {
            throw new RuntimeException("Cannot restore a backup that has been purged.");
        }

        String archiveLocation = job.getArchiveLocation();
        if (archiveLocation == null || archiveLocation.isEmpty()) {
            throw new RuntimeException("Archive location path is missing for this backup snapshot.");
        }

        File zipFile = new File(archiveLocation);
        if (!zipFile.exists()) {
            throw new RuntimeException("Backup zip file not found on disk at: " + archiveLocation);
        }

        log.warn("INITIATING DATABASE RESTORE from Backup ID: {}", backupId);

        // Snapshot current credentials BEFORE restore so user accounts/passwords are preserved
        List<String> preservedCredentials = snapshotCredentialsSql();

        Path stagingDir = Paths.get(backupLocation).resolve("restore_tmp_" + backupId);
        try {
            Files.createDirectories(stagingDir);

            // Unzip
            unzipFile(archiveLocation, stagingDir.toString());

            File[] sqlFiles = stagingDir.toFile().listFiles((d, name) -> name.endsWith(".sql"));
            if (sqlFiles == null || sqlFiles.length == 0) {
                throw new RuntimeException("No .sql file found inside backup zip archive.");
            }
            File sqlFile = sqlFiles[0];

            String url = dataSourceProperties.getUrl();
            String host = extractHost(url);
            String port = extractPort(url);
            String dbName = extractDatabaseName(url);
            String username = dataSourceProperties.getUsername();
            String password = dataSourceProperties.getPassword();

            // Try CLI restore first, fallback to robust JDBC importer
            boolean restored = false;
            try {
                List<String> restoreCmd = buildMysqlCmd(host, port, username, password, dbName);
                ProcessBuilder pb = new ProcessBuilder(restoreCmd);
                pb.redirectInput(sqlFile);
                pb.redirectErrorStream(true);
                Process p = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) log.info("[mysql restore] {}", line);
                }
                int exit = p.waitFor();
                if (exit == 0) {
                    restored = true;
                    log.info("mysql CLI restore completed successfully.");
                } else {
                    log.warn("mysql CLI restore exited with code {}, falling back to JDBC importer...", exit);
                }
            } catch (Exception cliEx) {
                log.warn("mysql CLI restore unavailable ({}), falling back to JDBC importer...", cliEx.getMessage());
            }

            if (!restored) {
                importDatabaseViaJdbc(sqlFile);
                log.info("JDBC database restore completed successfully.");
            }

            // Re-apply preserved credentials so active user accounts and passwords remain current
            reapplyCredentials(preservedCredentials);

            log.info("DATABASE RESTORE COMPLETED SUCCESSFULLY.");

        } catch (Exception e) {
            log.error("Database restore failed!", e);
            throw new RuntimeException("Restore failed: " + e.getMessage(), e);
        } finally {
            try {
                deleteDirectory(stagingDir.toFile());
            } catch (Exception ignored) {}
        }
    }

    private List<String> snapshotCredentialsSql() {
        List<String> sqlStatements = new ArrayList<>();
        String[] tables = new String[]{"permissions", "roles", "role_permissions", "users", "user_roles"};

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();

            for (String tableName : tables) {
                ResultSet rsTable = metaData.getTables(catalog, null, tableName, new String[]{"TABLE"});
                if (!rsTable.next()) {
                    continue;
                }

                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM `" + tableName + "`")) {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    int colCount = rsmd.getColumnCount();

                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("INSERT INTO `").append(tableName).append("` VALUES (");
                        for (int i = 1; i <= colCount; i++) {
                            if (i > 1) sb.append(", ");
                            Object val = rs.getObject(i);
                            if (val == null) {
                                sb.append("NULL");
                            } else if (val instanceof Number || val instanceof Boolean) {
                                sb.append(val);
                            } else if (val instanceof Timestamp ts) {
                                sb.append("'").append(ts.toString()).append("'");
                            } else if (val instanceof Date d) {
                                sb.append("'").append(d.toString()).append("'");
                            } else {
                                String str = val.toString().replace("\\", "\\\\").replace("'", "''");
                                sb.append("'").append(str).append("'");
                            }
                        }
                        sb.append(")");
                        sqlStatements.add(sb.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not snapshot credentials prior to restore: {}", e.getMessage());
        }

        return sqlStatements;
    }

    private void reapplyCredentials(List<String> credentialStatements) {
        if (credentialStatements == null || credentialStatements.isEmpty()) {
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 0");

                // Clear restored credentials tables
                st.execute("DELETE FROM `user_roles`");
                st.execute("DELETE FROM `role_permissions`");
                st.execute("DELETE FROM `users`");
                st.execute("DELETE FROM `roles`");
                st.execute("DELETE FROM `permissions`");

                for (String sql : credentialStatements) {
                    try {
                        st.execute(sql);
                    } catch (SQLException e) {
                        log.warn("Warning re-applying preserved credential SQL: {} -> {}", e.getMessage(), sql);
                    }
                }

                st.execute("SET FOREIGN_KEY_CHECKS = 1");
                conn.commit();
                log.info("Successfully re-applied current user credentials and permissions after database restore.");
            } catch (Exception e) {
                conn.rollback();
                log.error("Failed to re-apply preserved credentials after restore: {}", e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            log.error("Could not get connection to re-apply credentials: {}", e.getMessage(), e);
        }
    }

    // ─── Pure JDBC Backup & Restore Engine ───────────────────────────────────

    void exportDatabaseViaJdbc(File sqlFile) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sqlFile), StandardCharsets.UTF_8))) {
            
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            
            pw.println("-- BusinessManager Enterprise JDBC Database Dump");
            pw.println("-- Generated: " + LocalDateTime.now());
            pw.println("SET FOREIGN_KEY_CHECKS = 0;\n");

            ResultSet tables = metaData.getTables(catalog, null, "%", new String[]{"TABLE"});
            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (!tableName.startsWith("flyway_")) {
                    tableNames.add(tableName);
                }
            }

            for (String tableName : tableNames) {
                // Export CREATE TABLE
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SHOW CREATE TABLE `" + tableName + "`")) {
                    if (rs.next()) {
                        pw.println("DROP TABLE IF EXISTS `" + tableName + "`;");
                        pw.println(rs.getString(2) + ";\n");
                    }
                }

                // Export Rows
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM `" + tableName + "`")) {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    int colCount = rsmd.getColumnCount();
                    
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("INSERT INTO `").append(tableName).append("` VALUES (");
                        for (int i = 1; i <= colCount; i++) {
                            if (i > 1) sb.append(", ");
                            Object val = rs.getObject(i);
                            if (val == null) {
                                sb.append("NULL");
                            } else if (val instanceof Number || val instanceof Boolean) {
                                sb.append(val);
                            } else if (val instanceof Timestamp ts) {
                                sb.append("'").append(ts.toString()).append("'");
                            } else if (val instanceof Date d) {
                                sb.append("'").append(d.toString()).append("'");
                            } else {
                                String str = val.toString().replace("\\", "\\\\").replace("'", "''");
                                sb.append("'").append(str).append("'");
                            }
                        }
                        sb.append(");");
                        pw.println(sb.toString());
                    }
                    pw.println();
                }
            }

            pw.println("SET FOREIGN_KEY_CHECKS = 1;");
            pw.flush();
        }
    }

    private void importDatabaseViaJdbc(File sqlFile) throws Exception {
        try (Connection conn = dataSource.getConnection();
             BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sqlFile), StandardCharsets.UTF_8))) {
            
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 0");
                
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.startsWith("#")) {
                        continue;
                    }
                    sb.append(line).append("\n");
                    if (trimmed.endsWith(";")) {
                        String sql = sb.toString().trim();
                        if (sql.endsWith(";")) {
                            sql = sql.substring(0, sql.length() - 1);
                        }
                        if (!sql.isBlank()) {
                            try {
                                st.execute(sql);
                            } catch (SQLException e) {
                                log.warn("SQL statement warning during restore: {} -> {}", e.getMessage(), sql.length() > 80 ? sql.substring(0, 80) + "..." : sql);
                            }
                        }
                        sb.setLength(0);
                    }
                }
                
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    void runMysqlDump(String outputFilePath) throws IOException, InterruptedException {
        String url = dataSourceProperties.getUrl();
        String host = extractHost(url);
        String port = extractPort(url);
        String dbName = extractDatabaseName(url);
        String username = dataSourceProperties.getUsername();
        String password = dataSourceProperties.getPassword();

        List<String> command = new ArrayList<>();
        command.add(mysqldumpPath);
        if (host != null && !host.isEmpty()) { command.add("-h"); command.add(host); }
        if (port != null && !port.isEmpty()) { command.add("-P"); command.add(port); }
        command.add("-u"); command.add(username);
        if (password != null && !password.isEmpty()) command.add("-p" + password);
        command.add(dbName);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(new File(outputFilePath));
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = pb.start();
        
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("mysqldump execution timed out after 10 seconds");
        }
        int exit = process.exitValue();
        if (exit != 0) throw new RuntimeException("mysqldump failed with exit code " + exit);
    }

    void compressToZip(String sourceFilePath, String zipFilePath, String entryName) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(sourceFilePath)) {
            zos.putNextEntry(new ZipEntry(entryName));
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) >= 0) zos.write(buf, 0, len);
            zos.closeEntry();
        }
    }

    void unzipFile(String zipFilePath, String destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    new File(out.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = zis.read(buf)) > 0) fos.write(buf, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private List<String> buildMysqlCmd(String host, String port, String username, String password, String dbName) {
        List<String> cmd = new ArrayList<>();
        cmd.add(mysqlPath);
        if (host != null && !host.isEmpty()) { cmd.add("-h"); cmd.add(host); }
        if (port != null && !port.isEmpty()) { cmd.add("-P"); cmd.add(port); }
        cmd.add("-u"); cmd.add(username);
        if (password != null && !password.isEmpty()) cmd.add("-p" + password);
        cmd.add(dbName);
        return cmd;
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) deleteDirectory(f);
        dir.delete();
    }

    private String extractDatabaseName(String jdbcUrl) {
        Matcher m = Pattern.compile("jdbc:mysql://[^/]+/([^?]+)").matcher(jdbcUrl);
        return m.find() ? m.group(1) : "database";
    }

    private String extractHost(String jdbcUrl) {
        Matcher m = Pattern.compile("jdbc:mysql://([^:/]+)").matcher(jdbcUrl);
        return m.find() ? m.group(1) : "localhost";
    }

    private String extractPort(String jdbcUrl) {
        Matcher m = Pattern.compile("jdbc:mysql://[^:]+:(\\d+)").matcher(jdbcUrl);
        return m.find() ? m.group(1) : "3306";
    }

    private BackupJobDto mapToDto(BackupJob job) {
        return BackupJobDto.builder()
                .id(job.getId())
                .startTime(job.getStartTime())
                .endTime(job.getEndTime())
                .result(job.getResult() != null ? job.getResult().name() : null)
                .archiveLocation(job.getArchiveLocation())
                .fileSizeBytes(job.getFileSizeBytes())
                .verificationOutcome(job.getVerificationOutcome() != null ? job.getVerificationOutcome().name() : null)
                .errorMessage(job.getErrorMessage())
                .initiatedBy(job.getInitiatedBy())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
