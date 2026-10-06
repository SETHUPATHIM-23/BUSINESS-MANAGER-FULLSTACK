-- =============================================================================
-- BusinessManager Enterprise — PRODUCTION DATA RESET SCRIPT
-- =============================================================================
-- WARNING: THIS SCRIPT CLEARS ALL DEVELOPMENT, TEST, DEMO, AND TEMPORARY DATA
-- FROM THE DATABASE TO PREPARE FOR FRESH PRODUCTION DEPLOYMENT.
--
-- PRESERVED TABLES:
--   - flyway_schema_history (Database migration tracking)
--   - permissions (System RBAC permission definitions)
--   - roles (System RBAC roles)
--   - role_permissions (Role-to-permission mappings)
--   - accounts (Standard Chart of Accounts definitions)
--   - users / user_roles (Admin@senthur default admin user)
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Operational & Billing Transactions
TRUNCATE TABLE invoice_lines;
TRUNCATE TABLE invoices;

-- 2. Purchasing & Supplier Transactions
TRUNCATE TABLE goods_receipts;
TRUNCATE TABLE purchase_lines;
TRUNCATE TABLE purchase_orders;

-- 3. Inventory, Batches & Stock Movements
TRUNCATE TABLE stock_movements;
TRUNCATE TABLE inventory_transactions;
TRUNCATE TABLE product_batches;
TRUNCATE TABLE inventory_records;
TRUNCATE TABLE locations;
TRUNCATE TABLE product_categories;

-- 4. Fund Management & Financial Transactions
TRUNCATE TABLE reconciliation_records;
TRUNCATE TABLE fund_transactions;
TRUNCATE TABLE fund_accounts;

-- 5. Accounting & General Ledger
TRUNCATE TABLE journal_lines;
TRUNCATE TABLE journal_entries;
TRUNCATE TABLE account_mappings;
TRUNCATE TABLE fiscal_periods;

-- 6. Fleet, Logistics & Attendance
TRUNCATE TABLE delivery_assignments;
TRUNCATE TABLE maintenance_logs;
TRUNCATE TABLE trucks;
TRUNCATE TABLE attendance_records;

-- 7. Master Entities (Products, Customers, Suppliers, Employees)
TRUNCATE TABLE products;
TRUNCATE TABLE customers;
TRUNCATE TABLE suppliers;
TRUNCATE TABLE employees;

-- 8. Tax Configurations & Printers
TRUNCATE TABLE tax_rates;
TRUNCATE TABLE printer_supported_documents;
TRUNCATE TABLE printers;

-- 9. Operational Jobs, Reports & Logs
TRUNCATE TABLE print_jobs;
TRUNCATE TABLE scheduled_reports;
TRUNCATE TABLE report_definitions;
TRUNCATE TABLE dashboard_configs;
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE backup_jobs;

-- 10. User Accounts Cleanup (Preserve Admin@senthur)
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username != 'Admin@senthur');
DELETE FROM users WHERE username != 'Admin@senthur';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- POST-RESET INTEGRITY VERIFICATION QUERIES
-- =============================================================================
SELECT 'customers' AS table_name, COUNT(*) AS row_count FROM customers
UNION ALL SELECT 'suppliers', COUNT(*) FROM suppliers
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'employees', COUNT(*) FROM employees
UNION ALL SELECT 'trucks', COUNT(*) FROM trucks
UNION ALL SELECT 'invoices', COUNT(*) FROM invoices
UNION ALL SELECT 'purchase_orders', COUNT(*) FROM purchase_orders
UNION ALL SELECT 'fund_transactions', COUNT(*) FROM fund_transactions
UNION ALL SELECT 'journal_entries', COUNT(*) FROM journal_entries
UNION ALL SELECT 'stock_movements', COUNT(*) FROM stock_movements
UNION ALL SELECT 'print_jobs', COUNT(*) FROM print_jobs;
