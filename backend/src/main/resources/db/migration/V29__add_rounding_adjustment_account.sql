-- Add Rounding Adjustment Account
INSERT INTO accounts (code, name, type, created_by, updated_by, version) VALUES
('5200', 'Rounding Adjustment', 'EXPENSE', 'system', 'system', 0);

-- Map Rounding Adjustment Account
INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'ROUNDING_ADJUSTMENT', id, 'system', 'system', 0 FROM accounts WHERE code = '5200';
