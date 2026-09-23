-- ==============================================================
-- E-Bank26 Core Banking Management System - Database Schema (3NF)
-- Supports MySQL 8.0+ and PostgreSQL / H2
-- ==============================================================

-- 1. Roles Table
CREATE TABLE IF NOT EXISTS roles (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- 2. Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    failed_attempts INT DEFAULT 0,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_roles FOREIGN KEY (role_id) REFERENCES roles (role_id)
);

-- 3. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(50) NULL,
    entity_id BIGINT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Members Table
CREATE TABLE IF NOT EXISTS members (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_code VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    dob DATE NULL,
    gender VARCHAR(10) NULL,
    mobile VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100) NULL,
    address TEXT NULL,
    kyc_number VARCHAR(50) NULL,
    kyc_document_path VARCHAR(500) NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 5. Savings Accounts Table
CREATE TABLE IF NOT EXISTS savings_accounts (
    sb_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    interest_rate DECIMAL(5,2) DEFAULT 4.00,
    minimum_balance DECIMAL(15,2) DEFAULT 500.00,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    opened_date DATE NOT NULL,
    closed_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sb_member FOREIGN KEY (member_id) REFERENCES members (member_id)
);

-- 6. SB Transactions Table
CREATE TABLE IF NOT EXISTS sb_transactions (
    txn_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sb_id BIGINT NOT NULL,
    txn_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    narration VARCHAR(255) NULL,
    reference_number VARCHAR(50) NULL,
    created_by BIGINT NULL,
    txn_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sb_txn_account FOREIGN KEY (sb_id) REFERENCES savings_accounts (sb_id)
);

-- 7. Fixed Deposits Table
CREATE TABLE IF NOT EXISTS fixed_deposits (
    fd_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fd_number VARCHAR(20) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    linked_sb_id BIGINT NULL,
    principal DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    tenure_months INT NOT NULL,
    maturity_amount DECIMAL(15,2) NOT NULL,
    interest_type VARCHAR(20) DEFAULT 'SIMPLE',
    start_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    closed_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fd_member FOREIGN KEY (member_id) REFERENCES members (member_id),
    CONSTRAINT fk_fd_linked_sb FOREIGN KEY (linked_sb_id) REFERENCES savings_accounts (sb_id)
);

-- 8. Recurring Deposits Table
CREATE TABLE IF NOT EXISTS recurring_deposits (
    rd_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rd_number VARCHAR(20) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    linked_sb_id BIGINT NULL,
    monthly_amount DECIMAL(15,2) NOT NULL,
    tenure_months INT NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    maturity_amount DECIMAL(15,2) NOT NULL,
    start_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    closed_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rd_member FOREIGN KEY (member_id) REFERENCES members (member_id),
    CONSTRAINT fk_rd_linked_sb FOREIGN KEY (linked_sb_id) REFERENCES savings_accounts (sb_id)
);

-- 9. RD Transactions (Installments) Table
CREATE TABLE IF NOT EXISTS rd_transactions (
    txn_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rd_id BIGINT NOT NULL,
    installment_no INT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    penalty_amount DECIMAL(15,2) DEFAULT 0.00,
    due_date DATE NOT NULL,
    paid_date DATE NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rd_txn_account FOREIGN KEY (rd_id) REFERENCES recurring_deposits (rd_id)
);

-- 10. Loans Table
CREATE TABLE IF NOT EXISTS loans (
    loan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_number VARCHAR(20) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    loan_type VARCHAR(50) NOT NULL,
    principal DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    tenure_months INT NOT NULL,
    emi_amount DECIMAL(15,2) NOT NULL,
    outstanding_balance DECIMAL(15,2) NOT NULL,
    total_interest DECIMAL(15,2) NOT NULL,
    total_payable DECIMAL(15,2) NOT NULL,
    emi_type VARCHAR(20) DEFAULT 'REDUCING',
    start_date DATE NULL,
    end_date DATE NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    npa_flag BOOLEAN DEFAULT FALSE,
    overdue_amount DECIMAL(15,2) DEFAULT 0.00,
    approved_by BIGINT NULL,
    approved_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_loans_member FOREIGN KEY (member_id) REFERENCES members (member_id)
);

-- 11. Loan Schedules (Amortization) Table
CREATE TABLE IF NOT EXISTS loan_schedule (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    installment_no INT NOT NULL,
    due_date DATE NOT NULL,
    emi_amount DECIMAL(15,2) NOT NULL,
    principal_component DECIMAL(15,2) NOT NULL,
    interest_component DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    paid_status BOOLEAN DEFAULT FALSE,
    paid_date DATE NULL,
    penalty_amount DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_sched_loan FOREIGN KEY (loan_id) REFERENCES loans (loan_id)
);

-- 12. Loan Transactions Table
CREATE TABLE IF NOT EXISTS loan_transactions (
    txn_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    schedule_id BIGINT NULL,
    amount DECIMAL(15,2) NOT NULL,
    payment_date DATE NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    payment_type VARCHAR(20) DEFAULT 'EMI',
    penalty_amount DECIMAL(15,2) DEFAULT 0.00,
    narration VARCHAR(255) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_txn_loan FOREIGN KEY (loan_id) REFERENCES loans (loan_id),
    CONSTRAINT fk_loan_txn_sched FOREIGN KEY (schedule_id) REFERENCES loan_schedule (schedule_id)
);

-- 13. Share Capital Table
CREATE TABLE IF NOT EXISTS share_capital (
    share_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    certificate_number VARCHAR(20) NOT NULL UNIQUE,
    shares_count INT NOT NULL DEFAULT 0,
    share_value DECIMAL(10,2) NOT NULL DEFAULT 100.00,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_share_member FOREIGN KEY (member_id) REFERENCES members (member_id)
);

-- 14. Ledger Accounts (Chart of Accounts) Table
CREATE TABLE IF NOT EXISTS ledger_accounts (
    ledger_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ledger_code VARCHAR(50) NOT NULL UNIQUE,
    ledger_name VARCHAR(100) NOT NULL UNIQUE,
    ledger_type VARCHAR(20) NOT NULL,
    parent_id BIGINT NULL,
    opening_balance DECIMAL(15,2) DEFAULT 0.00,
    current_balance DECIMAL(15,2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_parent FOREIGN KEY (parent_id) REFERENCES ledger_accounts (ledger_id)
);

-- 15. Vouchers Table
CREATE TABLE IF NOT EXISTS vouchers (
    voucher_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_number VARCHAR(20) NOT NULL UNIQUE,
    voucher_type VARCHAR(20) NOT NULL,
    voucher_date DATE NOT NULL,
    narration TEXT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'POSTED',
    created_by BIGINT NULL,
    approved_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 16. Ledger Entries Table
CREATE TABLE IF NOT EXISTS ledger_entries (
    entry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    ledger_id BIGINT NOT NULL,
    debit DECIMAL(15,2) DEFAULT 0.00,
    credit DECIMAL(15,2) DEFAULT 0.00,
    narration VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entry_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers (voucher_id),
    CONSTRAINT fk_entry_ledger FOREIGN KEY (ledger_id) REFERENCES ledger_accounts (ledger_id)
);

-- Indexes for Optimal Performance
CREATE INDEX idx_members_mobile ON members (mobile);
CREATE INDEX idx_members_code ON members (member_code);
CREATE INDEX idx_sb_acc_no ON savings_accounts (account_number);
CREATE INDEX idx_fd_number ON fixed_deposits (fd_number);
CREATE INDEX idx_rd_number ON recurring_deposits (rd_number);
CREATE INDEX idx_loan_number ON loans (loan_number);
CREATE INDEX idx_voucher_number ON vouchers (voucher_number);
CREATE INDEX idx_sb_txn_date ON sb_transactions (txn_date);
CREATE INDEX idx_loan_sched_due ON loan_schedule (due_date);
