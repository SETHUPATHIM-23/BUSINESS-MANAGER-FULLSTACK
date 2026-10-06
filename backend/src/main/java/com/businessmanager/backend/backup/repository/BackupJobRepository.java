package com.businessmanager.backend.backup.repository;

import com.businessmanager.backend.backup.entity.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackupJobRepository extends JpaRepository<BackupJob, Long> {
    Optional<BackupJob> findFirstByOrderByCreatedAtDesc();
}
