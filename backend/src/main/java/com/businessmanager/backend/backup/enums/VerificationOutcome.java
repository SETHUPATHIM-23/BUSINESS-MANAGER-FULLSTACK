package com.businessmanager.backend.backup.enums;

public enum VerificationOutcome {
    PENDING,
    VALID,
    VERIFIED,   // used by BackupServiceImpl on success
    CORRUPT,
    FAILED,     // used by BackupServiceImpl on failure
    NOT_VERIFIED
}
