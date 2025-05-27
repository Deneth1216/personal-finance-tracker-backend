-- Ensure the database and schema/user exist if necessary before running this.
-- This script assumes a database named 'finance_tracker_db' might be used.
-- CREATE DATABASE IF NOT EXISTS finance_tracker_db;
-- USE finance_tracker_db;

-- Drop table if it exists to ensure a clean state for development
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Increased length for hashed passwords
    roles VARCHAR(255) NOT NULL,    -- e.g., 'ROLE_USER,ROLE_ADMIN'
    enabled BOOLEAN NOT NULL DEFAULT TRUE -- For Spring Security UserDetails
);

-- Optional: Add an index on username for faster lookups
CREATE INDEX idx_username ON users (username);
