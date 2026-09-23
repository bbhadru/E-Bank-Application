package com.ebank.service;

import com.ebank.dto.SavingsAccountRequest;
import com.ebank.dto.TransactionRequest;
import com.ebank.exception.InsufficientBalanceException;
import com.ebank.exception.InvalidOperationException;
import com.ebank.exception.ResourceNotFoundException;
import com.ebank.model.Member;
import com.ebank.model.SavingsAccount;
import com.ebank.model.SbTransaction;
import com.ebank.repository.MemberRepository;
import com.ebank.repository.SavingsAccountRepository;
import com.ebank.repository.SbTransactionRepository;
import com.ebank.util.AccountNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SavingsAccountService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SbTransactionRepository sbTransactionRepository;
    private final MemberRepository memberRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final VoucherService voucherService;
    private final AuditService auditService;

    public SavingsAccountService(SavingsAccountRepository savingsAccountRepository,
                                 SbTransactionRepository sbTransactionRepository,
                                 MemberRepository memberRepository,
                                 AccountNumberGenerator accountNumberGenerator,
                                 VoucherService voucherService,
                                 AuditService auditService) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.sbTransactionRepository = sbTransactionRepository;
        this.memberRepository = memberRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.voucherService = voucherService;
        this.auditService = auditService;
    }

    @Transactional
    public SavingsAccount createAccount(SavingsAccountRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + request.getMemberId()));

        Long maxId = savingsAccountRepository.findMaxId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        String accountNumber = accountNumberGenerator.generateSbAccountNumber(nextId);

        BigDecimal minBal = request.getMinimumBalance() != null ? request.getMinimumBalance() : new BigDecimal("500.00");
        BigDecimal intRate = request.getInterestRate() != null ? request.getInterestRate() : new BigDecimal("4.00");

        SavingsAccount account = SavingsAccount.builder()
                .accountNumber(accountNumber)
                .member(member)
                .balance(BigDecimal.ZERO)
                .interestRate(intRate)
                .minimumBalance(minBal)
                .status("ACTIVE")
                .openedDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        SavingsAccount saved = savingsAccountRepository.save(account);
        auditService.log(null, "SAVINGS_ACCOUNT_OPENED", "SavingsAccount", saved.getSbId(), null, accountNumber);
        return saved;
    }

    @Transactional
    public SbTransaction deposit(TransactionRequest request) {
        SavingsAccount account = savingsAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found with id: " + request.getAccountId()));

        if (!account.isActive()) {
            throw new InvalidOperationException("Savings account is not active");
        }

        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        savingsAccountRepository.save(account);

        SbTransaction txn = SbTransaction.builder()
                .savingsAccount(account)
                .txnType("DEPOSIT")
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .narration(request.getNarration() != null ? request.getNarration() : "Cash Deposit")
                .referenceNumber(request.getReferenceNumber())
                .txnDate(LocalDateTime.now())
                .build();

        SbTransaction savedTxn = sbTransactionRepository.save(txn);

        // Auto post double-entry voucher: Debit Cash, Credit Member Savings Deposits
        voucherService.postAutomatedVoucher(
                "RECEIPT",
                "Deposit to " + account.getAccountNumber() + " - " + txn.getNarration(),
                "CASH_ACCOUNT", request.getAmount(),
                "MEMBER_SAVINGS_DEPOSITS", request.getAmount()
        );

        auditService.log(null, "SB_DEPOSIT", "SavingsAccount", account.getSbId(), null, "Amount: " + request.getAmount());
        return savedTxn;
    }

    @Transactional
    public SbTransaction withdraw(TransactionRequest request) {
        SavingsAccount account = savingsAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found with id: " + request.getAccountId()));

        if (!account.isActive()) {
            throw new InvalidOperationException("Savings account is not active");
        }

        if (!account.hasSufficientBalance(request.getAmount())) {
            throw new InsufficientBalanceException("Insufficient balance. Account balance is " +
                    account.getBalance() + ", minimum required balance is " + account.getMinimumBalance());
        }

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        savingsAccountRepository.save(account);

        SbTransaction txn = SbTransaction.builder()
                .savingsAccount(account)
                .txnType("WITHDRAW")
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .narration(request.getNarration() != null ? request.getNarration() : "Cash Withdrawal")
                .referenceNumber(request.getReferenceNumber())
                .txnDate(LocalDateTime.now())
                .build();

        SbTransaction savedTxn = sbTransactionRepository.save(txn);

        // Auto post double-entry voucher: Debit Member Savings Deposits, Credit Cash
        voucherService.postAutomatedVoucher(
                "PAYMENT",
                "Withdrawal from " + account.getAccountNumber() + " - " + txn.getNarration(),
                "MEMBER_SAVINGS_DEPOSITS", request.getAmount(),
                "CASH_ACCOUNT", request.getAmount()
        );

        auditService.log(null, "SB_WITHDRAW", "SavingsAccount", account.getSbId(), null, "Amount: " + request.getAmount());
        return savedTxn;
    }

    @Transactional
    public void creditInterest(SavingsAccount account, BigDecimal interestAmount) {
        if (interestAmount.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal newBalance = account.getBalance().add(interestAmount);
        account.setBalance(newBalance);
        savingsAccountRepository.save(account);

        SbTransaction txn = SbTransaction.builder()
                .savingsAccount(account)
                .txnType("INTEREST")
                .amount(interestAmount)
                .balanceAfter(newBalance)
                .narration("Automated Interest Credit")
                .txnDate(LocalDateTime.now())
                .build();
        sbTransactionRepository.save(txn);

        // Auto post voucher: Debit Interest Expense, Credit Member Savings Deposits
        voucherService.postAutomatedVoucher(
                "JOURNAL",
                "Interest credit to " + account.getAccountNumber(),
                "INTEREST_EXPENSE_SAVINGS", interestAmount,
                "MEMBER_SAVINGS_DEPOSITS", interestAmount
        );
    }

    @Transactional
    public void closeAccount(Long accountId) {
        SavingsAccount account = savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found with id: " + accountId));

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidOperationException("Cannot close account with positive balance: " + account.getBalance());
        }

        account.setStatus("CLOSED");
        account.setClosedDate(LocalDate.now());
        savingsAccountRepository.save(account);
        auditService.log(null, "SAVINGS_ACCOUNT_CLOSED", "SavingsAccount", accountId, "ACTIVE", "CLOSED");
    }

    @Transactional(readOnly = true)
    public SavingsAccount getAccountById(Long accountId) {
        return savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found with id: " + accountId));
    }

    @Transactional(readOnly = true)
    public SavingsAccount getAccountByNumber(String accountNumber) {
        return savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found with number: " + accountNumber));
    }

    @Transactional(readOnly = true)
    public Page<SavingsAccount> searchAccounts(String query, String status, Pageable pageable) {
        return savingsAccountRepository.searchAccounts(query, status, pageable);
    }

    @Transactional(readOnly = true)
    public List<SavingsAccount> getAccountsByMember(Long memberId) {
        return savingsAccountRepository.findByMemberMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public List<SbTransaction> getAccountStatement(Long accountId, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            return sbTransactionRepository.findBySavingsAccountSbIdAndTxnDateBetweenOrderByTxnDateAsc(accountId, from, to);
        }
        return sbTransactionRepository.findBySavingsAccountSbIdOrderByTxnDateDesc(accountId);
    }
}
