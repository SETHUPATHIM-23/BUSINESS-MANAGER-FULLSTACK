-- V30: Seed missing permissions referenced in @PreAuthorize annotations
-- These permissions exist in controller code but were not seeded in V3.

INSERT IGNORE INTO permissions (code, description, created_by, updated_by)
VALUES
    ('BILLING_DELETE', 'Permission to void/delete billing invoices', 'system', 'system'),
    ('BILLING_POST',   'Permission to post/finalize billing invoices', 'system', 'system'),
    ('FINANCE_READ',   'Permission to view finance and P&L reports', 'system', 'system'),
    ('REPORTING_READ', 'Permission to view scheduled reports', 'system', 'system'),
    ('REPORTING_WRITE','Permission to create/edit scheduled reports', 'system', 'system');

-- Grant all newly added permissions to ROLE_ADMINISTRATOR
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMINISTRATOR'
  AND p.code IN ('BILLING_DELETE', 'BILLING_POST', 'FINANCE_READ', 'REPORTING_READ', 'REPORTING_WRITE');

-- Also grant REPORTING_READ/WRITE to ROLE_ACCOUNTANT (they run financial reports)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ACCOUNTANT'
  AND p.code IN ('REPORTING_READ', 'REPORTING_WRITE', 'FINANCE_READ');

-- Grant BILLING_POST and BILLING_DELETE to ROLE_ADMINISTRATOR (already covered above via CROSS JOIN)
-- Grant BILLING_POST to ROLE_SALES (they finalize invoices)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_SALES'
  AND p.code IN ('BILLING_POST', 'BILLING_DELETE');
