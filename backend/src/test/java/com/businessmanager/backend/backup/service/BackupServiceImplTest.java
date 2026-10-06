package com.businessmanager.backend.backup.service;

import com.businessmanager.backend.backup.entity.BackupJob;
import com.businessmanager.backend.backup.enums.BackupStatus;
import com.businessmanager.backend.backup.enums.VerificationOutcome;
import com.businessmanager.backend.backup.repository.BackupJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceImplTest {

    @Mock
    private BackupJobRepository backupJobRepository;

    @Mock
    private DataSourceProperties dataSourceProperties;

    @Mock
    private javax.sql.DataSource dataSource;

    private BackupServiceImpl backupService;
    private BackupServiceImpl backupServiceSpy;

    @BeforeEach
    void setUp() {
        backupService = new BackupServiceImpl(backupJobRepository, dataSourceProperties, dataSource);
        backupServiceSpy = spy(backupService);

        ReflectionTestUtils.setField(backupServiceSpy, "backupEnabled", true);
        ReflectionTestUtils.setField(backupServiceSpy, "backupLocation", "./test_backups");
        ReflectionTestUtils.setField(backupServiceSpy, "maxKeep", 5);

        lenient().when(dataSourceProperties.getUrl()).thenReturn("jdbc:mysql://localhost:3306/testdb");
        lenient().when(backupJobRepository.save(any(BackupJob.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void performBackup_Success() throws Exception {
        doNothing().when(backupServiceSpy).runMysqlDump(anyString());
        doAnswer(invocation -> {
            String zipPath = invocation.getArgument(1);
            Files.createDirectories(Path.of("./test_backups"));
            Files.writeString(Path.of(zipPath), "dummy zip content");
            return null;
        }).when(backupServiceSpy).compressToZip(anyString(), anyString(), anyString());

        when(backupJobRepository.findAll(any(Sort.class))).thenReturn(List.of());

        backupServiceSpy.performBackup();

        ArgumentCaptor<BackupJob> captor = ArgumentCaptor.forClass(BackupJob.class);
        verify(backupJobRepository, atLeastOnce()).save(captor.capture());

        BackupJob finalJob = captor.getValue();
        assertEquals(BackupStatus.SUCCESS, finalJob.getResult());
        assertEquals(VerificationOutcome.VERIFIED, finalJob.getVerificationOutcome());
        assertNotNull(finalJob.getEndTime());
    }

    @Test
    void performBackup_Failure_OnDumpError() throws Exception {
        doThrow(new RuntimeException("mysqldump not found"))
                .when(backupServiceSpy).runMysqlDump(anyString());
        doThrow(new RuntimeException("JDBC export failed"))
                .when(backupServiceSpy).exportDatabaseViaJdbc(any(File.class));

        backupServiceSpy.performBackup();

        ArgumentCaptor<BackupJob> captor = ArgumentCaptor.forClass(BackupJob.class);
        verify(backupJobRepository, atLeastOnce()).save(captor.capture());

        BackupJob finalJob = captor.getValue();
        assertEquals(BackupStatus.FAILURE, finalJob.getResult());
        assertEquals(VerificationOutcome.FAILED, finalJob.getVerificationOutcome());
    }

    @Test
    void pruneOldBackups_DeletesOldestBeyondMaxKeep() {
        List<BackupJob> jobs = new java.util.ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            BackupJob j = new BackupJob();
            j.setId((long) i);
            j.setResult(BackupStatus.SUCCESS);
            j.setArchiveLocation(null);
            jobs.add(j);
        }

        when(backupJobRepository.findAll(any(Sort.class))).thenReturn(jobs);

        try {
            var method = BackupServiceImpl.class.getDeclaredMethod("pruneOldBackups");
            method.setAccessible(true);
            method.invoke(backupServiceSpy);
        } catch (Exception e) {
            fail("pruneOldBackups threw: " + e.getMessage());
        }

        ArgumentCaptor<BackupJob> captor = ArgumentCaptor.forClass(BackupJob.class);
        verify(backupJobRepository, times(1)).save(captor.capture());

        BackupJob purged = captor.getValue();
        assertEquals(BackupStatus.PURGED, purged.getResult());
        assertEquals(6L, purged.getId());
    }
}
