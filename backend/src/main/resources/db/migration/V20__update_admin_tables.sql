-- Update users table to match Prompt 136 requirements
ALTER TABLE users CHANGE COLUMN password password_hash VARCHAR(100) NOT NULL;
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP NULL DEFAULT NULL;

-- Update permissions table to match Prompt 136 requirements
ALTER TABLE permissions CHANGE COLUMN name code VARCHAR(50) NOT NULL;
ALTER TABLE permissions ADD COLUMN description VARCHAR(255) NULL;

-- Make audit_logs compatible with BaseEntity
ALTER TABLE audit_logs ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE audit_logs ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE audit_logs ADD COLUMN created_by VARCHAR(50);
ALTER TABLE audit_logs ADD COLUMN updated_by VARCHAR(50);
ALTER TABLE audit_logs ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
