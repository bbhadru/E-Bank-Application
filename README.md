# E-Bank26 – Core Banking Management System
> Web-based core banking management system using layered MVC + REST architecture with automated financial processing.

---

## 1. Project Overview

**E-Bank26** is a core banking web application designed for cooperative banks, credit societies, and micro-financial institutions. It features automated financial processing including daily interest calculation, loan EMI schedules (reducing balance and flat rate), fixed deposit maturity transfers, double-entry voucher & general ledger posting, and financial statements (Trial Balance, Profit & Loss).

### Technology Stack
- **Backend**: Java 21, Spring Boot 4 / Spring Framework, Spring Security, Spring Data JPA, Hibernate, JJWT
- **Frontend**: Thymeleaf, HTML5, CSS3, Bootstrap 5, Bootstrap Icons, Chart.js
- **Database**: H2 (In-memory development default) / MySQL 8.0+
- **Reporting**: Apache POI (Excel .xlsx export), LibrePDF OpenPDF (PDF export)
- **Build Tools**: Gradle 9.x (wrapper provided) & Maven (`pom.xml`)
- **Containers**: Docker & Docker Compose

---

## 2. Default Credentials & Roles

| Role | Username | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `admin123` | Full access to all modules, user administration, system settings |
| **STAFF** | `staff` | `staff123` | Member registration, savings account transactions, deposits, loan origination |
| **ACCOUNTANT**| `accountant` | `accountant123`| Double-entry vouchers, ledger books, trial balance, and financial reports |

---

## 3. Key Modules & Automation Design

1. **User Authentication & Authorization**:
   - BCrypt password hashing.
   - Dual authentication support: Form-based session login for web UI + JWT Bearer token authentication for `/api/**` REST endpoints.
   - Account lockout after 5 consecutive failed login attempts.
   - Audit trail logging for all sensitive user actions.

2. **Dashboard (Real-Time Metrics & Charts)**:
   - Live aggregated KPI metrics: Total Members, Deposits, Loan Outstanding, Today's Transactions.
   - Automated system alerts for overdue loan EMIs, maturing fixed deposits within 30 days, and pending approvals.
   - Chart.js visual trends for monthly deposits and loan recoveries.

3. **Member Management**:
   - Automated Member ID generation (`MEM-00001`).
   - Duplicate member detection by mobile and KYC numbers.
   - One-click automatic Savings Account provisioning during member registration.

4. **Savings Accounts (SB)**:
   - Automated account number generation (`SB10000001`).
   - Transaction validation with minimum balance enforcement (₹ 500 default).
   - Automated double-entry voucher generation for deposits and withdrawals (Dr/Cr Cash & Member Savings Deposits).
   - Real-time passbook statements and daily interest accrual calculation.

5. **Fixed Deposits (FD)**:
   - Automated FD certificate numbering (`FD20000001`).
   - Supports Simple and Compound Interest calculations.
   - Automated maturity date and maturity amount computation.
   - Automated maturity payout crediting to linked Savings Account.

6. **Recurring Deposits (RD)**:
   - Automated RD numbering (`RD30000001`).
   - Monthly installment schedule generation with due dates.
   - Penalty calculation for overdue installments.
   - Automated maturity settlement to linked Savings Account.

7. **Loan Management & EMI Automation**:
   - Reducing balance (`E = P * r * (1+r)^n / ((1+r)^n - 1)`) and Flat Rate EMI computation.
   - Complete monthly amortization schedule generation upon loan disbursement.
   - Automated loan disbursement crediting to member's Savings Account or Cash.
   - Automatic repayment allocation (penalty -> interest component -> principal component).
   - Automated NPA (Non-Performing Asset) classification for installments overdue > 90 days.

8. **Share Capital Module**:
   - Member share allotment with certificate numbers (`SH50000001`).
   - Automated dividend distribution calculation with automatic credits to members' Savings Accounts.

9. **Double-Entry Accounting & Ledger Books**:
   - Validation that `Total Debits == Total Credits` on every voucher.
   - Standard chart of accounts (Cash, Bank, Member Savings, FD Liabilities, Loan Assets, Interest Incomes, etc.).
   - Automated real-time Trial Balance verification (`isBalanced = true`).
   - Automated Profit & Loss statement computation.

10. **Reports & Exports**:
    - Member Register, Savings Statement, FD Register, RD Schedule, Loan Register, Trial Balance.
    - One-click export to **Excel (.xlsx)** and **PDF (.pdf)**.

11. **Scheduled Batch Jobs**:
    - Daily midnight cron (`0 0 0 * * ?`): calculates and credits daily savings interest, auto-matures FDs, and flags NPA loans.

---

## 4. How to Run

### Option A: Running with Gradle (Recommended)

1. Make sure Java 21 is installed (`java -version`).
2. Run the application:
   ```bash
   ./gradlew bootRun
   ```
3. Open your browser and navigate to:
   ```
   http://localhost:8080
   ```
4. Log in using `admin` / `admin123`.

### Option B: Running with Docker Compose (MySQL + App)

1. Navigate to the `docker/` directory:
   ```bash
   cd docker
   docker-compose up --build
   ```
2. The application will start on `http://localhost:8080` with a persistent MySQL 8.0 container.

### Option C: Running with Maven

```bash
mvn spring-boot:run
```

---

## 5. REST API Documentation

Swagger / OpenAPI documentation is accessible at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Sample REST Requests

#### 1. Authentication (JWT Login)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```
*Response:*
```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "admin",
  "role": "ROLE_ADMIN",
  "expiresIn": 86400000
}
```

#### 2. Get Dashboard Summary
```bash
curl -X GET http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer <token>"
```

#### 3. Post Savings Deposit
```bash
curl -X POST http://localhost:8080/api/accounts/savings/deposit \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"accountId": 1, "txnType": "DEPOSIT", "amount": 5000.00, "narration": "Counter deposit"}'
```

---

## 6. Project Directory Structure

```
e-bank26/
├── src/
│   ├── main/
│   │   ├── java/com/ebank/
│   │   │   ├── EBankApplication.java
│   │   │   ├── config/          # Security, Web, Scheduler, DataInitializer
│   │   │   ├── controller/      # Web MVC and REST Controllers
│   │   │   ├── dto/             # Requests and Response DTOs
│   │   │   ├── exception/       # Custom Exceptions and GlobalExceptionHandler
│   │   │   ├── model/           # JPA Entities (Member, Account, Loan, Voucher, etc.)
│   │   │   ├── repository/      # Spring Data JPA Repositories
│   │   │   ├── scheduler/       # ScheduledTasks Cron runner
│   │   │   ├── security/        # JWT Provider, Auth Filter, UserPrincipal
│   │   │   ├── service/         # Business Logic & Financial Automations
│   │   │   └── util/            # Account Generator, EMIUtils, InterestCalculator, Exporter
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/          # CSS (style.css, dashboard.css), JS (main, validation, chart)
│   │       └── templates/       # Thymeleaf HTML Templates
│   └── test/java/com/ebank/     # Automated Unit & Integration Tests
├── database/                    # schema.sql and data.sql
├── docker/                      # Dockerfile and docker-compose.yml
├── build.gradle                 # Gradle Build Specification
├── pom.xml                      # Maven Build Specification
└── README.md
```
