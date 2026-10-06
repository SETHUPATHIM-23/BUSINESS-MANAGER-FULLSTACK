-- =====================================================================
-- V11: Journal entries table for double-entry accounting posts
-- Supports the posting engine required by BILL-040 and future modules
-- =====================================================================

CREATE TABLE journal_entries (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_date        DATE           NOT NULL,
    reference_type    VARCHAR(30)    NOT NULL,
    reference_id      BIGINT         NOT NULL,
    reference_number  VARCHAR(50),
    description       VARCHAR(255),
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    version           BIGINT         NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journal_entry_lines (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    journal_entry_id  BIGINT         NOT NULL,
    account_id        BIGINT         NOT NULL,
    debit_amount      DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    credit_amount     DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    description       VARCHAR(255),
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_jel_journal_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_jel_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for fast lookups by reference
CREATE INDEX idx_je_reference ON journal_entries (reference_type, reference_id);

-- =====================================================================
-- Inventory tracking: stock_on_hand column on products table
-- and inventory_transactions for movement audit trail
-- =====================================================================

ALTER TABLE products ADD COLUMN stock_on_hand DECIMAL(15, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE products ADD COLUMN allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE inventory_transactions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id        BIGINT         NOT NULL,
    transaction_type  VARCHAR(30)    NOT NULL,
    reference_type    VARCHAR(30)    NOT NULL,
    reference_id      BIGINT         NOT NULL,
    quantity_change   DECIMAL(15, 2) NOT NULL,
    stock_after       DECIMAL(15, 2) NOT NULL,
    notes             VARCHAR(255),
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_invtx_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_invtx_product ON inventory_transactions (product_id);
CREATE INDEX idx_invtx_reference ON inventory_transactions (reference_type, reference_id);
