package com.ebank.service;

import com.ebank.dto.LoanRepaymentRequest;
import com.ebank.dto.LoanRequest;
import com.ebank.dto.TransactionRequest;
import com.ebank.exception.InvalidOperationException;
import com.ebank.exception.ResourceNotFoundException;
import com.ebank.model.*;
import com.ebank.repository.LoanRepository;
import com.ebank.repository.LoanScheduleRepository;
import com.ebank.repository.LoanTransactionRepository;
import com.ebank.repository.MemberRepository;
import com.ebank.util.AccountNumberGenerator;
import com.ebank.util.EMIUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountService savingsAccountService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final VoucherService voucherService;
    private final AuditService auditService;

    public LoanService(LoanRepository loanRepository,
                       LoanScheduleRepository loanScheduleRepository,
                       LoanTransactionRepository loanTransactionRepository,
                       MemberRepository memberRepository,
                       SavingsAccountService savingsAccountService,
                       AccountNumberGenerator accountNumberGenerator,
                       VoucherService voucherService,
                       AuditService auditService) {
        this.loanRepository = loanRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.loanTransactionRepository = loanTransactionRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountService = savingsAccountService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.voucherService = voucherService;
        this.auditService = auditService;
    }

    @Transactional
    public Loan applyLoan(LoanRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + request.getMemberId()));

        Long maxId = loanRepository.findMaxId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        String loanNumber = accountNumberGenerator.generateLoanNumber(nextId);

        BigDecimal emi = "FLAT".equalsIgnoreCase(request.getEmiType()) ?
                EMIUtils.calculateFlatEMI(request.getPrincipal(), request.getInterestRate(), request.getTenureMonths()) :
                EMIUtils.calculateReducingEMI(request.getPrincipal(), request.getInterestRate(), request.getTenureMonths());

        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(request.getTenureMonths()));
        BigDecimal totalInterest = totalPayable.subtract(request.getPrincipal());

        Loan loan = Loan.builder()
                .loanNumber(loanNumber)
                .member(member)
                .loanType(request.getLoanType())
                .principal(request.getPrincipal())
                .interestRate(request.getInterestRate())
                .tenureMonths(request.getTenureMonths())
                .emiAmount(emi)
                .outstandingBalance(request.getPrincipal())
                .totalInterest(totalInterest)
                .totalPayable(totalPayable)
                .emiType(request.getEmiType() != null ? request.getEmiType() : "REDUCING")
                .status("PENDING")
                .npaFlag(false)
                .overdueAmount(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Loan saved = loanRepository.save(loan);
        auditService.log(null, "LOAN_APPLIED", "Loan", saved.getLoanId(), null, loanNumber);
        return saved;
    }

    @Transactional
    public Loan approveLoan(Long loanId, Long approverId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        if (!"PENDING".equalsIgnoreCase(loan.getStatus())) {
            throw new InvalidOperationException("Only pending loans can be approved");
        }

        loan.setStatus("APPROVED");
        loan.setApprovedBy(approverId != null ? approverId : 1L);
        loan.setApprovedDate(LocalDate.now());
        loan.setUpdatedAt(LocalDateTime.now());

        Loan updated = loanRepository.save(loan);
        auditService.log(approverId, "LOAN_APPROVED", "Loan", loanId, "PENDING", "APPROVED");
        return updated;
    }

    @Transactional
    public Loan disburseLoan(Long loanId, Long targetSbAccountId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        if (!"APPROVED".equalsIgnoreCase(loan.getStatus())) {
            throw new InvalidOperationException("Loan must be approved before disbursement");
        }

        LocalDate now = LocalDate.now();
        loan.setStartDate(now);
        loan.setEndDate(now.plusMonths(loan.getTenureMonths()));
        loan.setStatus("ACTIVE");
        loan.setOutstandingBalance(loan.getPrincipal());
        loan.setUpdatedAt(LocalDateTime.now());

        // Generate and save amortization schedule
        List<LoanSchedule> schedules = EMIUtils.generateSchedule(loan);
        loanScheduleRepository.saveAll(schedules);

        Loan disbursed = loanRepository.save(loan);

        // Credit to linked SB account if specified, else cash
        String creditAccount = "CASH_ACCOUNT";
        if (targetSbAccountId != null) {
            SavingsAccount sbAccount = savingsAccountService.getAccountById(targetSbAccountId);
            if (sbAccount != null && sbAccount.isActive()) {
                savingsAccountService.deposit(TransactionRequest.builder()
                        .accountId(targetSbAccountId)
                        .amount(loan.getPrincipal())
                        .narration("Disbursement of Loan: " + loan.getLoanNumber())
                        .build());
                creditAccount = "MEMBER_SAVINGS_DEPOSITS";
            }
        }

        // Post accounting voucher: Debit Loan Asset, Credit Cash / SB
        voucherService.postAutomatedVoucher(
                "PAYMENT",
                "Disbursement for Loan: " + loan.getLoanNumber(),
                "LOANS_ADVANCES_ASSET", loan.getPrincipal(),
                creditAccount, loan.getPrincipal()
        );

        auditService.log(null, "LOAN_DISBURSED", "Loan", loanId, "APPROVED", "ACTIVE");
        return disbursed;
    }

    @Transactional
    public LoanTransaction processRepayment(LoanRepaymentRequest request) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + request.getLoanId()));

        if (!loan.isActive()) {
            throw new InvalidOperationException("Loan is not active");
        }

        BigDecimal amount = request.getAmount();
        List<LoanSchedule> unpaidSchedules = loanScheduleRepository
                .findByLoanLoanIdAndPaidStatusFalseOrderByInstallmentNoAsc(loan.getLoanId());

        LoanSchedule currentSchedule = unpaidSchedules.isEmpty() ? null : unpaidSchedules.get(0);
        BigDecimal interestPortion = BigDecimal.ZERO;
        BigDecimal principalPortion = amount;

        if (currentSchedule != null) {
            interestPortion = currentSchedule.getInterestComponent();
            if (amount.compareTo(currentSchedule.getEmiAmount()) >= 0) {
                currentSchedule.setPaidStatus(true);
                currentSchedule.setPaidDate(LocalDate.now());
                loanScheduleRepository.save(currentSchedule);
            }
            if (amount.compareTo(interestPortion) > 0) {
                principalPortion = amount.subtract(interestPortion);
            } else {
                interestPortion = amount;
                principalPortion = BigDecimal.ZERO;
            }
        }

        BigDecimal newBalance = loan.getOutstandingBalance().subtract(principalPortion);
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            newBalance = BigDecimal.ZERO;
            loan.setStatus("CLOSED");
        }
        loan.setOutstandingBalance(newBalance);
        loan.setUpdatedAt(LocalDateTime.now());
        loanRepository.save(loan);

        LoanTransaction txn = LoanTransaction.builder()
                .loan(loan)
                .schedule(currentSchedule)
                .amount(amount)
                .paymentDate(LocalDate.now())
                .balanceAfter(newBalance)
                .paymentType(request.getPaymentType() != null ? request.getPaymentType() : "EMI")
                .penaltyAmount(BigDecimal.ZERO)
                .narration(request.getNarration() != null ? request.getNarration() : "Loan EMI Payment")
                .createdAt(LocalDateTime.now())
                .build();

        LoanTransaction savedTxn = loanTransactionRepository.save(txn);

        // Auto post double-entry voucher: Debit Cash, Credit Loan Asset (Principal) + Credit Interest Income (Interest)
        if (principalPortion.compareTo(BigDecimal.ZERO) > 0) {
            voucherService.postAutomatedVoucher(
                    "RECEIPT",
                    "Principal Repayment for " + loan.getLoanNumber(),
                    "CASH_ACCOUNT", principalPortion,
                    "LOANS_ADVANCES_ASSET", principalPortion
            );
        }
        if (interestPortion.compareTo(BigDecimal.ZERO) > 0) {
            voucherService.postAutomatedVoucher(
                    "RECEIPT",
                    "Interest Collection for " + loan.getLoanNumber(),
                    "CASH_ACCOUNT", interestPortion,
                    "INTEREST_INCOME_LOANS", interestPortion
            );
        }

        auditService.log(null, "LOAN_REPAYMENT", "Loan", loan.getLoanId(), null, "Amount: " + amount);
        return savedTxn;
    }

    @Transactional
    public void checkAndFlagNpa(LocalDate currentDate) {
        List<LoanSchedule> overdueList = loanScheduleRepository.findOverdueSchedules(currentDate);
        for (LoanSchedule schedule : overdueList) {
            long daysOverdue = ChronoUnit.DAYS.between(schedule.getDueDate(), currentDate);
            if (daysOverdue > 90) {
                Loan loan = schedule.getLoan();
                if (!Boolean.TRUE.equals(loan.getNpaFlag())) {
                    loan.setNpaFlag(true);
                    loan.setStatus("NPA");
                    loanRepository.save(loan);
                    auditService.log(null, "LOAN_FLAGGED_NPA", "Loan", loan.getLoanId(), "ACTIVE", "NPA");
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<Loan> searchLoans(String query, String status, Pageable pageable) {
        return loanRepository.searchLoans(query, status, pageable);
    }

    @Transactional(readOnly = true)
    public Loan getLoanById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
    }

    @Transactional(readOnly = true)
    public List<LoanSchedule> getAmortizationSchedule(Long loanId) {
        return loanScheduleRepository.findByLoanLoanIdOrderByInstallmentNoAsc(loanId);
    }

    @Transactional(readOnly = true)
    public List<LoanTransaction> getLoanTransactions(Long loanId) {
        return loanTransactionRepository.findByLoanLoanIdOrderByPaymentDateDesc(loanId);
    }

    @Transactional(readOnly = true)
    public List<Loan> getLoansByMember(Long memberId) {
        return loanRepository.findByMemberMemberId(memberId);
    }
}
