CREATE TABLE backup_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    result VARCHAR(50) NOT NULL,
    archive_location VARCHAR(500),
    file_size_bytes BIGINT,
    verification_outcome VARCHAR(50) NOT NULL,
    error_message TEXT,
    initiated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_backup_jobs_result ON backup_jobs(result);
CREATE INDEX idx_backup_jobs_start_time ON backup_jobs(start_time);
