-- =====================================================================
-- V10: Billing (Sales Invoicing) tables — invoices + invoice_lines
-- Traces to: BILL-010, BILL-020
-- =====================================================================

CREATE TABLE invoices (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number    VARCHAR(50)    NOT NULL UNIQUE,
    customer_id       BIGINT         NOT NULL,
    invoice_date      DATE           NOT NULL,
    due_date          DATE           NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    subtotal          DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tax_total         DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    grand_total       DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    amount_paid       DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    notes             TEXT,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invoice_lines (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id        BIGINT         NOT NULL,
    product_id        BIGINT         NOT NULL,
    description       VARCHAR(255),
    quantity          DECIMAL(15, 2) NOT NULL DEFAULT 1.00,
    unit_price        DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    discount          DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount        DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    line_total        DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_invoice_line_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_line_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index on customer FK for fast lookups
CREATE INDEX idx_invoice_customer ON invoices (customer_id);

-- Index on invoice FK for fast line lookups
CREATE INDEX idx_invoice_line_invoice ON invoice_lines (invoice_id);

-- Index on status for filtered queries
CREATE INDEX idx_invoice_status ON invoices (status);
