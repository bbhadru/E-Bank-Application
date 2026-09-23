-- ==============================================================
-- E-Bank26 Core Banking Management System - Seed Data
-- ==============================================================

-- 1. Seed Roles
INSERT INTO roles (role_id, role_name) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_STAFF'),
(3, 'ROLE_ACCOUNTANT')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 2. Seed Default Users (Password: admin123, staff123, accountant123 - BCrypt encoded)
-- BCrypt for admin123: $2a$10$7EqJtq98hPqEX7fNZaFWoO.8/iF4N1gQzYQW6JvU1wRzQ2C5wQ1hG
INSERT INTO users (user_id, username, password_hash, role_id, status, failed_attempts, created_at) VALUES
(1, 'admin', '$2a$10$kUmvG6vN1HntK02wFKhRf.nvxr0Yo.GmmoiJbnJo1y./PXlmwgNyG', 1, 'ACTIVE', 0, CURRENT_TIMESTAMP),
(2, 'staff', '$2a$10$kUmvG6vN1HntK02wFKhRf.nvxr0Yo.GmmoiJbnJo1y./PXlmwgNyG', 2, 'ACTIVE', 0, CURRENT_TIMESTAMP),
(3, 'accountant', '$2a$10$kUmvG6vN1HntK02wFKhRf.nvxr0Yo.GmmoiJbnJo1y./PXlmwgNyG', 3, 'ACTIVE', 0, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- 3. Seed Chart of Accounts (Double-Entry General Ledger)
INSERT INTO ledger_accounts (ledger_id, ledger_code, ledger_name, ledger_type, opening_balance, current_balance, is_active) VALUES
(1, 'CASH_ACCOUNT', 'Cash on Hand', 'ASSET', 500000.00, 500000.00, TRUE),
(2, 'BANK_ACCOUNT', 'Reserve Bank Operating Account', 'ASSET', 1000000.00, 1000000.00, TRUE),
(3, 'LOANS_ADVANCES_ASSET', 'Loans & Advances to Members', 'ASSET', 0.00, 0.00, TRUE),
(4, 'MEMBER_SAVINGS_DEPOSITS', 'Member Savings Deposits (SB)', 'LIABILITY', 0.00, 0.00, TRUE),
(5, 'FIXED_DEPOSIT_LIABILITIES', 'Fixed Term Deposits (FD)', 'LIABILITY', 0.00, 0.00, TRUE),
(6, 'RECURRING_DEPOSIT_LIABILITIES', 'Recurring Deposits (RD)', 'LIABILITY', 0.00, 0.00, TRUE),
(7, 'SHARE_CAPITAL_LIABILITY', 'Member Share Capital', 'LIABILITY', 0.00, 0.00, TRUE),
(8, 'INTEREST_INCOME_LOANS', 'Interest Income on Loans', 'INCOME', 0.00, 0.00, TRUE),
(9, 'PENALTY_INCOME', 'Late Payment Penalty Income', 'INCOME', 0.00, 0.00, TRUE),
(10, 'INTEREST_EXPENSE_SAVINGS', 'Interest Expense on Savings', 'EXPENSE', 0.00, 0.00, TRUE),
(11, 'INTEREST_EXPENSE_FD', 'Interest Expense on Fixed Deposits', 'EXPENSE', 0.00, 0.00, TRUE),
(12, 'DIVIDEND_EXPENSE', 'Dividend Paid to Members', 'EXPENSE', 0.00, 0.00, TRUE)
ON DUPLICATE KEY UPDATE ledger_name = VALUES(ledger_name);

-- 4. Seed Initial Member
INSERT INTO members (member_id, member_code, full_name, dob, gender, mobile, email, address, kyc_number, status, created_at) VALUES
(1, 'MEM-00001', 'Rajesh Sharma', '1985-05-15', 'MALE', '9876543210', 'rajesh.sharma@example.com', '12, MG Road, Bangalore', 'AADHAAR-9876-5432-1098', 'ACTIVE', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- 5. Seed Initial Savings Account
INSERT INTO savings_accounts (sb_id, account_number, member_id, balance, interest_rate, minimum_balance, status, opened_date, created_at) VALUES
(1, 'SB10000001', 1, 15000.00, 4.00, 500.00, 'ACTIVE', '2026-01-01', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE balance = VALUES(balance);

-- 6. Initial Opening Balance Voucher
INSERT INTO vouchers (voucher_id, voucher_number, voucher_type, voucher_date, narration, amount, status, created_at) VALUES
(1, 'VCH-2026-00001', 'RECEIPT', '2026-01-01', 'Initial Deposit for Member SB10000001', 15000.00, 'POSTED', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE narration = VALUES(narration);

INSERT INTO ledger_entries (entry_id, voucher_id, ledger_id, debit, credit, narration) VALUES
(1, 1, 1, 15000.00, 0.00, 'Cash received for initial SB deposit'),
(2, 1, 4, 0.00, 15000.00, 'Deposit credited to Member Savings Deposits')
ON DUPLICATE KEY UPDATE narration = VALUES(narration);
