-- =====================================================================
-- V18: Truck Management tables — trucks, delivery_assignments, maintenance_logs
-- Traces to: TRUCK-010, TRUCK-020
-- =====================================================================

CREATE TABLE trucks (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    registration_number VARCHAR(50)    NOT NULL UNIQUE,
    make                 VARCHAR(100)   NOT NULL,
    model                VARCHAR(100)   NOT NULL,
    capacity             DOUBLE,
    fuel_type            VARCHAR(50),
    driver_employee_id   BIGINT,
    last_service_date    DATE,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           VARCHAR(50),
    updated_by           VARCHAR(50),
    version              BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_truck_driver_employee FOREIGN KEY (driver_employee_id) REFERENCES employees (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE delivery_assignments (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    truck_id             BIGINT         NOT NULL,
    invoice_id           BIGINT,
    status               VARCHAR(50)    NOT NULL DEFAULT 'ASSIGNED',
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           VARCHAR(50),
    updated_by           VARCHAR(50),
    version              BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_delivery_assignment_truck FOREIGN KEY (truck_id) REFERENCES trucks (id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_assignment_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE maintenance_logs (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    truck_id             BIGINT         NOT NULL,
    date                 DATE           NOT NULL,
    type                 VARCHAR(100)   NOT NULL,
    cost                 DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    odometer_reading     DOUBLE,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           VARCHAR(50),
    updated_by           VARCHAR(50),
    version              BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_maintenance_log_truck FOREIGN KEY (truck_id) REFERENCES trucks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for performance
CREATE INDEX idx_trucks_registration ON trucks (registration_number);
CREATE INDEX idx_delivery_assignments_truck ON delivery_assignments (truck_id);
CREATE INDEX idx_delivery_assignments_invoice ON delivery_assignments (invoice_id);
CREATE INDEX idx_maintenance_logs_truck ON maintenance_logs (truck_id);
