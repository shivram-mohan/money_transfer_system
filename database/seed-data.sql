-- Money Transfer System - Seed Data
-- Passwords are BCrypt encoded. Raw password for all users: password123

USE money_transfer_db;

-- ─── SEED ACCOUNTS ─────────────────────────────────────────────────
-- BCrypt hash of 'password123': $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO accounts (holder_name, username, password, balance, status, version) VALUES
('John Smith',   'john',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 50000.00, 'ACTIVE', 0),
('Jane Doe',     'jane',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 30000.00, 'ACTIVE', 0),
('Bob Wilson',   'bob',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 15000.00, 'ACTIVE', 0),
('Alice Brown',  'alice',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 75000.00, 'ACTIVE', 0);

-- Note: Admin user (admin/admin123) is configured in-memory via SecurityConfig.java
-- and does not need a row in the accounts table.
