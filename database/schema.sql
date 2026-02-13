-- Money Transfer System - Database Schema
-- MySQL 8.x

CREATE DATABASE IF NOT EXISTS money_transfer_db;
USE money_transfer_db;

-- ─── ACCOUNTS TABLE ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS accounts (
    id          BIGINT          PRIMARY KEY AUTO_INCREMENT,
    holder_name VARCHAR(255)    NOT NULL,
    username    VARCHAR(255)    UNIQUE,
    password    VARCHAR(255),
    balance     DECIMAL(18,2)   NOT NULL,
    status      VARCHAR(20)     NOT NULL,
    version     INT             DEFAULT 0,
    last_updated TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ─── TRANSACTION_LOGS TABLE ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transaction_logs (
    id              VARCHAR(36)     PRIMARY KEY,
    from_account    BIGINT          NOT NULL,
    to_account      BIGINT          NOT NULL,
    amount          DECIMAL(18,2)   NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    failure_reason  VARCHAR(255),
    idempotency_key VARCHAR(100)    UNIQUE,
    created_on      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (from_account) REFERENCES accounts(id),
    FOREIGN KEY (to_account) REFERENCES accounts(id)
);

-- ─── INDEXES ───────────────────────────────────────────────────────
CREATE INDEX idx_accounts_username ON accounts(username);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_txn_from_account ON transaction_logs(from_account);
CREATE INDEX idx_txn_to_account ON transaction_logs(to_account);
CREATE INDEX idx_txn_idempotency ON transaction_logs(idempotency_key);
