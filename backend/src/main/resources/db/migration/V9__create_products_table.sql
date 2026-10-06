CREATE TABLE product_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tax_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    rate DECIMAL(5, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category_id BIGINT,
    unit_of_measure VARCHAR(20) NOT NULL,
    cost_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    base_selling_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tax_rate_id BIGINT,
    reorder_level DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    reorder_qty DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    batch_tracked BOOLEAN NOT NULL DEFAULT FALSE,
    include_in_financial_calculations BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_categories (id),
    CONSTRAINT fk_product_tax_rate FOREIGN KEY (tax_rate_id) REFERENCES tax_rates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default categories and tax rates
INSERT INTO product_categories (name, description) VALUES ('Raw Materials', 'Primary stock used in production');
INSERT INTO product_categories (name, description) VALUES ('Finished Goods', 'Manufactured products ready for sale');

INSERT INTO tax_rates (name, rate) VALUES ('Standard VAT', 15.00);
INSERT INTO tax_rates (name, rate) VALUES ('Zero Rated', 0.00);
