CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    parent_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_account_parent FOREIGN KEY (parent_id) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE account_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mapping_key VARCHAR(50) NOT NULL UNIQUE,
    account_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_mapping_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1. Seed Minimal Standard Chart of Accounts
INSERT INTO accounts (code, name, type, created_by, updated_by, version) VALUES
('1010', 'Cash on Hand', 'ASSET', 'system', 'system', 0),
('1020', 'Bank Current Account', 'ASSET', 'system', 'system', 0),
('1100', 'Accounts Receivable', 'ASSET', 'system', 'system', 0),
('1200', 'Inventory Asset', 'ASSET', 'system', 'system', 0),
('1300', 'Tax Receivable (Input)', 'ASSET', 'system', 'system', 0),
('2100', 'Accounts Payable', 'LIABILITY', 'system', 'system', 0),
('2200', 'Tax Payable (Output)', 'LIABILITY', 'system', 'system', 0),
('3000', 'Capital / Retained Earnings', 'EQUITY', 'system', 'system', 0),
('4000', 'Sales Revenue', 'INCOME', 'system', 'system', 0),
('5000', 'Cost of Goods Sold', 'EXPENSE', 'system', 'system', 0),
('5100', 'Sundry Expense', 'EXPENSE', 'system', 'system', 0);

-- 2. Seed Account Mappings linking logical keys to accounts
INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'CASH', id, 'system', 'system', 0 FROM accounts WHERE code = '1010';

INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'BANK', id, 'system', 'system', 0 FROM accounts WHERE code = '1020';

INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'ACCOUNTS_RECEIVABLE', id, 'system', 'system', 0 FROM accounts WHERE code = '1100';

INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'INVENTORY_ASSET', id, 'system', 'system', 0 FROM accounts WHERE code = '1200';

INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'TAX_RECEIVABLE', id, 'system', 'system', 0 FROM accounts WHERE code = '1300';

INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'ACCOUNTS_PAYABLE', id, 'system', 'system', 0 FROM accounts WHERE code = '2100';

INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'TAX_PAYABLE', id, 'system', 'system', 0 FROM accounts WHERE code = '2200';

INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'EQUITY', id, 'system', 'system', 0 FROM accounts WHERE code = '3000';

INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'SALES_REVENUE', id, 'system', 'system', 0 FROM accounts WHERE code = '4000';

INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'COST_OF_GOODS_SOLD', id, 'system', 'system', 0 FROM accounts WHERE code = '5000';

INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'SUNDRY_EXPENSE', id, 'system', 'system', 0 FROM accounts WHERE code = '5100';
