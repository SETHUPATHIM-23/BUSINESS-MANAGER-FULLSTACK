DROP TABLE IF EXISTS user_roles;

CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1. Seed Permissions
INSERT INTO permissions (name, created_by, updated_by) VALUES
('CUSTOMER_READ', 'system', 'system'), ('CUSTOMER_WRITE', 'system', 'system'),
('SUPPLIER_READ', 'system', 'system'), ('SUPPLIER_WRITE', 'system', 'system'),
('PRODUCT_READ', 'system', 'system'), ('PRODUCT_WRITE', 'system', 'system'),
('BILLING_READ', 'system', 'system'), ('BILLING_WRITE', 'system', 'system'),
('PURCHASE_READ', 'system', 'system'), ('PURCHASE_WRITE', 'system', 'system'),
('INVENTORY_READ', 'system', 'system'), ('INVENTORY_WRITE', 'system', 'system'),
('ACCOUNTING_READ', 'system', 'system'), ('ACCOUNTING_WRITE', 'system', 'system'),
('EMPLOYEE_READ', 'system', 'system'), ('EMPLOYEE_WRITE', 'system', 'system'),
('TRUCK_READ', 'system', 'system'), ('TRUCK_WRITE', 'system', 'system'),
('FUND_READ', 'system', 'system'), ('FUND_WRITE', 'system', 'system'),
('SYSTEM_READ', 'system', 'system'), ('SYSTEM_WRITE', 'system', 'system');

-- 2. Seed Roles
INSERT INTO roles (name, created_by, updated_by) VALUES
('ROLE_ADMINISTRATOR', 'system', 'system'),
('ROLE_ACCOUNTANT', 'system', 'system'),
('ROLE_SALES', 'system', 'system'),
('ROLE_PURCHASE', 'system', 'system'),
('ROLE_WAREHOUSE', 'system', 'system'),
('ROLE_HR', 'system', 'system'),
('ROLE_AUDITOR', 'system', 'system');

-- 3. Link Permissions to Roles
-- Helper variables for IDs
-- Administrator: All permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'ROLE_ADMINISTRATOR';

-- Accountant: Accounting, Funds, and read-only on everything else
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'ROLE_ACCOUNTANT' 
  AND (p.name IN ('ACCOUNTING_READ', 'ACCOUNTING_WRITE', 'FUND_READ', 'FUND_WRITE') 
       OR p.name LIKE '%_READ');

-- Sales / Billing Staff: Customer read/write, Billing read/write, Product read
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'ROLE_SALES' 
  AND p.name IN ('CUSTOMER_READ', 'CUSTOMER_WRITE', 'BILLING_READ', 'BILLING_WRITE', 'PRODUCT_READ');

-- Purchase / Inventory Staff: Supplier, Purchase, Inventory, Product read/write
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'ROLE_PURCHASE' 
  AND p.name IN ('SUPPLIER_READ', 'SUPPLIER_WRITE', 'PURCHASE_READ', 'PURCHASE_WRITE', 'INVENTORY_READ', 'INVENTORY_WRITE', 'PRODUCT_READ', 'PRODUCT_WRITE');

-- Warehouse / Logistics Staff: Inventory read/write (transfers), Truck read/write, Product read
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'ROLE_WAREHOUSE' 
  AND p.name IN ('INVENTORY_READ', 'INVENTORY_WRITE', 'TRUCK_READ', 'TRUCK_WRITE', 'PRODUCT_READ');

-- HR / Employee Manager: Employee read/write, Truck read
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'ROLE_HR' 
  AND p.name IN ('EMPLOYEE_READ', 'EMPLOYEE_WRITE', 'TRUCK_READ');

-- Read-only / Auditor: All READ permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'ROLE_AUDITOR' AND p.name LIKE '%_READ';

-- 4. Map the default seed admin user (from V2 migration) to ROLE_ADMINISTRATOR
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r 
WHERE u.username IN ('sethu', 'admin') AND r.name = 'ROLE_ADMINISTRATOR';
