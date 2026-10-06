CREATE TABLE system_settings (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    installation_state     VARCHAR(30)  NOT NULL DEFAULT 'NOT_INITIALIZED',
    company_name           VARCHAR(255) NULL,
    legal_name             VARCHAR(255) NULL,
    address                TEXT         NULL,
    city                   VARCHAR(100) NULL,
    state                  VARCHAR(100) NULL,
    state_code             VARCHAR(10)  NULL,
    pincode                VARCHAR(20)  NULL,
    country                VARCHAR(100) NULL DEFAULT 'India',
    mobile                 VARCHAR(50)  NULL,
    telephone              VARCHAR(50)  NULL,
    email                  VARCHAR(100) NULL,
    website                VARCHAR(255) NULL,
    gstin                  VARCHAR(50)  NULL,
    pan                    VARCHAR(50)  NULL,
    invoice_prefix         VARCHAR(20)  NOT NULL DEFAULT 'INV-',
    invoice_next_number    BIGINT       NOT NULL DEFAULT 1,
    currency_symbol        VARCHAR(10)  NOT NULL DEFAULT '₹',
    currency_code          VARCHAR(10)  NOT NULL DEFAULT 'INR',
    invoice_terms          TEXT         NULL,
    invoice_footer         TEXT         NULL,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             VARCHAR(50)  NULL,
    updated_by             VARCHAR(50)  NULL,
    version                BIGINT       NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert initial initialized row
INSERT INTO system_settings (id, installation_state, created_by, updated_by, version)
VALUES (1, 'INITIALIZED', 'system', 'system', 0);
