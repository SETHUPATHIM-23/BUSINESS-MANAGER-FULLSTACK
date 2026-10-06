-- =====================================================================
-- V14: Recreation of accounting journal tables with updated columns
-- Traces to: ACC-010 (Accounting Core)
-- =====================================================================

DROP TABLE IF EXISTS journal_entry_lines;
DROP TABLE IF EXISTS journal_entries;

CREATE TABLE journal_entries (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_date         DATE           NOT NULL,
    reference          VARCHAR(100)   NOT NULL,
    memo               VARCHAR(255),
    source_module      VARCHAR(50),
    source_document_id BIGINT,
    fiscal_period_id   BIGINT,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by         VARCHAR(50),
    updated_by         VARCHAR(50),
    version            BIGINT         NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journal_lines (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    journal_entry_id   BIGINT         NOT NULL,
    account_id         BIGINT         NOT NULL,
    debit_amount       DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    credit_amount      DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    description        VARCHAR(255),
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by         VARCHAR(50),
    updated_by         VARCHAR(50),
    version            BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_jl_journal_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_jl_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_je_source ON journal_entries (source_module, source_document_id);
