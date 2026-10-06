-- =====================================================================
-- V26: Product Batches
-- Traces to: PROD-070
-- =====================================================================

CREATE TABLE product_batches (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id        BIGINT         NOT NULL,
    batch_number      VARCHAR(100)   NOT NULL,
    expiry_date       DATE,
    stock_on_hand     DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_batch_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    UNIQUE KEY uk_product_batch (product_id, batch_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_batch_product ON product_batches (product_id);
CREATE INDEX idx_batch_expiry ON product_batches (expiry_date);
