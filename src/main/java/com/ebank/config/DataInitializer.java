package com.ebank.config;

import com.ebank.model.*;
import com.ebank.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final VoucherRepository voucherRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           MemberRepository memberRepository,
                           SavingsAccountRepository savingsAccountRepository,
                           LedgerAccountRepository ledgerAccountRepository,
                           VoucherRepository voucherRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.voucherRepository = voucherRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing E-Bank26 default seed data...");

        // 1. Roles
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_ADMIN").build()));

        Role staffRole = roleRepository.findByRoleName("ROLE_STAFF")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_STAFF").build()));

        Role accountantRole = roleRepository.findByRoleName("ROLE_ACCOUNTANT")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_ACCOUNTANT").build()));

        // 2. Default Users
        if (!userRepository.existsByUserName("admin")) {
            User admin = User.builder()
                    .userName("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .status("ACTIVE")
                    .failedAttempts(0)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            log.info("Created default admin user: admin / admin123");
        }

        if (!userRepository.existsByUserName("staff")) {
            User staff = User.builder()
                    .userName("staff")
                    .passwordHash(passwordEncoder.encode("staff123"))
                    .role(staffRole)
                    .status("ACTIVE")
                    .failedAttempts(0)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(staff);
            log.info("Created default staff user: staff / staff123");
        }

        if (!userRepository.existsByUserName("accountant")) {
            User accountant = User.builder()
                    .userName("accountant")
                    .passwordHash(passwordEncoder.encode("accountant123"))
                    .role(accountantRole)
                    .status("ACTIVE")
                    .failedAttempts(0)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(accountant);
            log.info("Created default accountant user: accountant / accountant123");
        }

        // 3. Chart of Accounts
        createLedgerAccountIfMissing("CASH_ACCOUNT", "Cash on Hand", "ASSET", new BigDecimal("500000.00"));
        createLedgerAccountIfMissing("BANK_ACCOUNT", "Reserve Bank Operating Account", "ASSET", new BigDecimal("1000000.00"));
        createLedgerAccountIfMissing("LOANS_ADVANCES_ASSET", "Loans & Advances to Members", "ASSET", BigDecimal.ZERO);
        createLedgerAccountIfMissing("MEMBER_SAVINGS_DEPOSITS", "Member Savings Deposits (SB)", "LIABILITY", BigDecimal.ZERO);
        createLedgerAccountIfMissing("FIXED_DEPOSIT_LIABILITIES", "Fixed Term Deposits (FD)", "LIABILITY", BigDecimal.ZERO);
        createLedgerAccountIfMissing("RECURRING_DEPOSIT_LIABILITIES", "Recurring Deposits (RD)", "LIABILITY", BigDecimal.ZERO);
        createLedgerAccountIfMissing("SHARE_CAPITAL_LIABILITY", "Member Share Capital", "LIABILITY", BigDecimal.ZERO);
        createLedgerAccountIfMissing("INTEREST_INCOME_LOANS", "Interest Income on Loans", "INCOME", BigDecimal.ZERO);
        createLedgerAccountIfMissing("PENALTY_INCOME", "Late Payment Penalty Income", "INCOME", BigDecimal.ZERO);
        createLedgerAccountIfMissing("INTEREST_EXPENSE_SAVINGS", "Interest Expense on Savings", "EXPENSE", BigDecimal.ZERO);
        createLedgerAccountIfMissing("INTEREST_EXPENSE_FD", "Interest Expense on Fixed Deposits", "EXPENSE", BigDecimal.ZERO);
        createLedgerAccountIfMissing("DIVIDEND_EXPENSE", "Dividend Paid to Members", "EXPENSE", BigDecimal.ZERO);

        // 4. Sample Member & Savings Account
        if (memberRepository.count() == 0) {
            Member member = Member.builder()
                    .memberCode("MEM-00001")
                    .fullName("Rajesh Sharma")
                    .dob(LocalDate.of(1985, 5, 15))
                    .gender("MALE")
                    .mobile("9876543210")
                    .email("rajesh.sharma@example.com")
                    .address("12, MG Road, Bangalore")
                    .kycNumber("AADHAAR-9876-5432-1098")
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .build();
            Member savedMember = memberRepository.save(member);

            SavingsAccount account = SavingsAccount.builder()
                    .accountNumber("SB10000001")
                    .member(savedMember)
                    .balance(new BigDecimal("15000.00"))
                    .interestRate(new BigDecimal("4.00"))
                    .minimumBalance(new BigDecimal("500.00"))
                    .status("ACTIVE")
                    .openedDate(LocalDate.now().minusMonths(3))
                    .createdAt(LocalDateTime.now().minusMonths(3))
                    .build();
            savingsAccountRepository.save(account);

            // Seed Initial Opening Voucher
            Voucher voucher = Voucher.builder()
                    .voucherNumber("VCH-2026-00001")
                    .voucherType("RECEIPT")
                    .voucherDate(LocalDate.now().minusMonths(3))
                    .narration("Opening Deposit for Member SB10000001")
                    .amount(new BigDecimal("15000.00"))
                    .status("POSTED")
                    .createdAt(LocalDateTime.now().minusMonths(3))
                    .build();
            Voucher savedVoucher = voucherRepository.save(voucher);

            LedgerAccount cashAcc = ledgerAccountRepository.findByLedgerCode("CASH_ACCOUNT").orElse(null);
            LedgerAccount sbAcc = ledgerAccountRepository.findByLedgerCode("MEMBER_SAVINGS_DEPOSITS").orElse(null);

            if (cashAcc != null && sbAcc != null) {
                ledgerEntryRepository.save(LedgerEntry.builder()
                        .voucher(savedVoucher)
                        .ledgerAccount(cashAcc)
                        .debit(new BigDecimal("15000.00"))
                        .credit(BigDecimal.ZERO)
                        .narration("Cash received for opening deposit")
                        .createdAt(LocalDateTime.now().minusMonths(3))
                        .build());

                ledgerEntryRepository.save(LedgerEntry.builder()
                        .voucher(savedVoucher)
                        .ledgerAccount(sbAcc)
                        .debit(BigDecimal.ZERO)
                        .credit(new BigDecimal("15000.00"))
                        .narration("Credited to Member Savings Deposits")
                        .createdAt(LocalDateTime.now().minusMonths(3))
                        .build());
            }

            log.info("Created sample member Rajesh Sharma and SB account SB10000001");
        }

        log.info("E-Bank26 initialization completed successfully.");
    }

    private void createLedgerAccountIfMissing(String code, String name, String type, BigDecimal opening) {
        if (ledgerAccountRepository.findByLedgerCode(code).isEmpty()) {
            ledgerAccountRepository.save(LedgerAccount.builder()
                    .ledgerCode(code)
                    .ledgerName(name)
                    .ledgerType(type)
                    .openingBalance(opening)
                    .currentBalance(opening)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }
}
