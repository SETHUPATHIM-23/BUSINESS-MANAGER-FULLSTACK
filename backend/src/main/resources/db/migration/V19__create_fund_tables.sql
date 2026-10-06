-- V19: Create Fund Management tables (fund_accounts, fund_transactions, reconciliation_records)

CREATE TABLE fund_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL, -- CASH, BANK
    account_number VARCHAR(50) UNIQUE,
    current_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    gl_account_id BIGINT,
    location_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL,
    created_by VARCHAR(100) NULL,
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_fund_account_gl FOREIGN KEY (gl_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_fund_account_location FOREIGN KEY (location_id) REFERENCES locations(id)
);

CREATE TABLE fund_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_account_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL, -- RECEIPT, PAYMENT, TRANSFER
    amount DECIMAL(15, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    reference_document_type VARCHAR(50) NULL,
    reference_document_id BIGINT NULL,
    target_fund_account_id BIGINT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NULL,
    created_by VARCHAR(100) NULL,
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_fund_tx_account FOREIGN KEY (fund_account_id) REFERENCES fund_accounts(id),
    CONSTRAINT fk_fund_tx_target_account FOREIGN KEY (target_fund_account_id) REFERENCES fund_accounts(id)
);

CREATE TABLE reconciliation_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_transaction_id BIGINT NOT NULL,
    bank_statement_line_ref VARCHAR(100) NOT NULL,
    matched BOOLEAN NOT NULL DEFAULT FALSE,
    reconciled_at TIMESTAMP NULL,
    reconciled_by VARCHAR(100) NULL,
    created_at TIMESTAMP NULL,
    created_by VARCHAR(100) NULL,
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reconciliation_tx FOREIGN KEY (fund_transaction_id) REFERENCES fund_transactions(id)
);
