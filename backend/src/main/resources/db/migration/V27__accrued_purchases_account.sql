-- =====================================================================
-- V27: Accrued Purchases (GRNI) Account Foundation
-- Traces to: PURCH-050, PURCH-070
-- =====================================================================

-- 1. Create the Accrued Purchases liability account
INSERT INTO accounts (code, name, type, active, created_by, updated_by, version)
VALUES ('2110', 'Accrued Purchases (GRNI)', 'LIABILITY', true, 'system', 'system', 0);

-- 2. Seed the Account Mapping
INSERT INTO account_mappings (mapping_key, account_id, created_by, updated_by, version)
SELECT 'ACCRUED_PURCHASES', id, 'system', 'system', 0 FROM accounts WHERE code = '2110';
