CREATE TABLE printers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    cups_identifier VARCHAR(200),
    location VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE printer_supported_documents (
    printer_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (printer_id, document_type),
    CONSTRAINT fk_printer_doc_types FOREIGN KEY (printer_id) REFERENCES printers(id) ON DELETE CASCADE
);

CREATE TABLE print_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_type VARCHAR(50) NOT NULL,
    source_module VARCHAR(50) NOT NULL,
    source_document_id BIGINT,
    document_name VARCHAR(200) NOT NULL,
    document_data LONGBLOB,
    printer_id BIGINT,
    status VARCHAR(30) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_print_jobs_printer FOREIGN KEY (printer_id) REFERENCES printers(id)
);
