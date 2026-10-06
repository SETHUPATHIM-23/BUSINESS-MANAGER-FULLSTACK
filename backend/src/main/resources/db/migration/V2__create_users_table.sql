CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default Administrator account (username: sethu, password: Sethupathi#*$1)
-- BCrypt hash for 'Sethupathi#*$1': $2b$10$KzQZoBWvU1gD2FxcnGTXROqhGjvs5nvB4WftG88ooXSQIYDgsIasy
INSERT INTO users (username, password, created_at, updated_at, created_by, updated_by, version)
VALUES ('sethu', '$2b$10$KzQZoBWvU1gD2FxcnGTXROqhGjvs5nvB4WftG88ooXSQIYDgsIasy', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0);

SET @admin_id = LAST_INSERT_ID();

INSERT INTO user_roles (user_id, role)
VALUES (@admin_id, 'ROLE_ADMINISTRATOR');
