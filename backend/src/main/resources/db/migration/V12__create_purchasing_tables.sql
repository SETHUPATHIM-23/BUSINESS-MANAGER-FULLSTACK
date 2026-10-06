-- =====================================================================
-- V12: Purchasing module tables — purchase_orders, purchase_lines, goods_receipts
-- Traces to: PURCH-010
-- =====================================================================

CREATE TABLE purchase_orders (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_number         VARCHAR(50)    NOT NULL UNIQUE,
    supplier_id       BIGINT         NOT NULL,
    order_date        DATE           NOT NULL,
    expected_delivery_date DATE,
    notes             TEXT,
    status            VARCHAR(30)    NOT NULL DEFAULT 'DRAFT',
    amount_paid       DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE purchase_lines (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_id             BIGINT         NOT NULL,
    product_id        BIGINT         NOT NULL,
    ordered_qty       DECIMAL(15, 2) NOT NULL DEFAULT 1.00,
    cost_price        DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount        DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    line_total        DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_po_line_po FOREIGN KEY (po_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_po_line_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE goods_receipts (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_id             BIGINT         NOT NULL,
    purchase_line_id  BIGINT         NOT NULL,
    received_date     DATE           NOT NULL,
    received_qty      DECIMAL(15, 2) NOT NULL DEFAULT 1.00,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_gr_po FOREIGN KEY (po_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_gr_line FOREIGN KEY (purchase_line_id) REFERENCES purchase_lines (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for fast lookups
CREATE INDEX idx_po_supplier ON purchase_orders (supplier_id);
CREATE INDEX idx_po_line_po ON purchase_lines (po_id);
CREATE INDEX idx_gr_po ON goods_receipts (po_id);
CREATE INDEX idx_gr_line ON goods_receipts (purchase_line_id);
