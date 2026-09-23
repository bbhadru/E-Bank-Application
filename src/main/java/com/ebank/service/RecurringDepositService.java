package com.ebank.service;

import com.ebank.dto.RecurringDepositRequest;
import com.ebank.dto.TransactionRequest;
import com.ebank.exception.InvalidOperationException;
import com.ebank.exception.ResourceNotFoundException;
import com.ebank.model.Member;
import com.ebank.model.RdTransaction;
import com.ebank.model.RecurringDeposit;
import com.ebank.model.SavingsAccount;
import com.ebank.repository.MemberRepository;
import com.ebank.repository.RdTransactionRepository;
import com.ebank.repository.RecurringDepositRepository;
import com.ebank.repository.SavingsAccountRepository;
import com.ebank.util.AccountNumberGenerator;
import com.ebank.util.InterestCalculator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecurringDepositService {

    private final RecurringDepositRepository recurringDepositRepository;
    private final RdTransactionRepository rdTransactionRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsAccountService savingsAccountService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final VoucherService voucherService;
    private final AuditService auditService;

    public RecurringDepositService(RecurringDepositRepository recurringDepositRepository,
                                   RdTransactionRepository rdTransactionRepository,
                                   MemberRepository memberRepository,
                                   SavingsAccountRepository savingsAccountRepository,
                                   SavingsAccountService savingsAccountService,
                                   AccountNumberGenerator accountNumberGenerator,
                                   VoucherService voucherService,
                                   AuditService auditService) {
        this.recurringDepositRepository = recurringDepositRepository;
        this.rdTransactionRepository = rdTransactionRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsAccountService = savingsAccountService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.voucherService = voucherService;
        this.auditService = auditService;
    }

    @Transactional
    public RecurringDeposit createRecurringDeposit(RecurringDepositRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + request.getMemberId()));

        SavingsAccount linkedSb = null;
        if (request.getLinkedSbId() != null) {
            linkedSb = savingsAccountRepository.findById(request.getLinkedSbId()).orElse(null);
        }

        Long maxId = recurringDepositRepository.findMaxId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        String rdNumber = accountNumberGenerator.generateRdNumber(nextId);

        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(request.getTenureMonths());

        BigDecimal maturityAmount = InterestCalculator.calculateRdMaturityAmount(
                request.getMonthlyAmount(), request.getInterestRate(), request.getTenureMonths());

        RecurringDeposit rd = RecurringDeposit.builder()
                .rdNumber(rdNumber)
                .member(member)
                .linkedSavingsAccount(linkedSb)
                .monthlyAmount(request.getMonthlyAmount())
                .tenureMonths(request.getTenureMonths())
                .interestRate(request.getInterestRate())
                .maturityAmount(maturityAmount)
                .startDate(startDate)
                .maturityDate(maturityDate)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        RecurringDeposit saved = recurringDepositRepository.save(rd);

        // Generate Installment Schedule
        List<RdTransaction> transactions = new ArrayList<>();
        for (int i = 1; i <= request.getTenureMonths(); i++) {
            RdTransaction txn = RdTransaction.builder()
                    .recurringDeposit(saved)
                    .installmentNo(i)
                    .amount(request.getMonthlyAmount())
                    .penaltyAmount(BigDecimal.ZERO)
                    .dueDate(startDate.plusMonths(i))
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            transactions.add(txn);
        }
        rdTransactionRepository.saveAll(transactions);

        auditService.log(null, "RD_CREATED", "RecurringDeposit", saved.getRdId(), null, rdNumber);
        return saved;
    }

    @Transactional
    public RdTransaction payInstallment(Long rdId, Integer installmentNo, BigDecimal penalty) {
        RecurringDeposit rd = recurringDepositRepository.findById(rdId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring deposit not found with id: " + rdId));

        if (!rd.isActive()) {
            throw new InvalidOperationException("Recurring deposit is not active");
        }

        List<RdTransaction> installments = rdTransactionRepository.findByRecurringDepositRdIdOrderByInstallmentNoAsc(rdId);
        RdTransaction target = installments.stream()
                .filter(t -> t.getInstallmentNo().equals(installmentNo))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Installment #" + installmentNo + " not found"));

        if ("PAID".equalsIgnoreCase(target.getStatus())) {
            throw new InvalidOperationException("Installment #" + installmentNo + " is already paid");
        }

        LocalDate now = LocalDate.now();
        BigDecimal penaltyToApply = penalty != null ? penalty : BigDecimal.ZERO;
        if (penaltyToApply.compareTo(BigDecimal.ZERO) == 0 && now.isAfter(target.getDueDate())) {
            // Auto penalty: 1% of installment
            penaltyToApply = target.getAmount().multiply(new BigDecimal("0.01")).setScale(2, java.math.RoundingMode.HALF_UP);
        }

        target.setPaidDate(now);
        target.setStatus("PAID");
        target.setPenaltyAmount(penaltyToApply);
        RdTransaction saved = rdTransactionRepository.save(target);

        // Auto post double-entry voucher: Debit Cash, Credit RD Liability (+ Credit Penalty Income if any)
        BigDecimal totalPaid = target.getAmount().add(penaltyToApply);
        voucherService.postAutomatedVoucher(
                "RECEIPT",
                "RD Installment #" + installmentNo + " for " + rd.getRdNumber(),
                "CASH_ACCOUNT", totalPaid,
                "RECURRING_DEPOSIT_LIABILITIES", totalPaid
        );

        auditService.log(null, "RD_INSTALLMENT_PAID", "RecurringDeposit", rdId, null, "Installment #" + installmentNo);
        return saved;
    }

    @Transactional
    public void closeOrMatureDeposit(Long rdId) {
        RecurringDeposit rd = recurringDepositRepository.findById(rdId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring deposit not found with id: " + rdId));

        if (!rd.isActive()) {
            throw new InvalidOperationException("Recurring deposit is already closed");
        }

        rd.setStatus("CLOSED");
        rd.setClosedDate(LocalDate.now());
        recurringDepositRepository.save(rd);

        // Auto transfer to linked SB if present
        if (rd.getLinkedSavingsAccount() != null && rd.getLinkedSavingsAccount().isActive()) {
            savingsAccountService.deposit(TransactionRequest.builder()
                    .accountId(rd.getLinkedSavingsAccount().getSbId())
                    .amount(rd.getMaturityAmount())
                    .narration("Recurring Deposit Maturity Payout: " + rd.getRdNumber())
                    .build());
        }

        auditService.log(null, "RD_CLOSED", "RecurringDeposit", rdId, "ACTIVE", "CLOSED");
    }

    @Transactional(readOnly = true)
    public Page<RecurringDeposit> searchDeposits(String query, String status, Pageable pageable) {
        return recurringDepositRepository.searchDeposits(query, status, pageable);
    }

    @Transactional(readOnly = true)
    public RecurringDeposit getDepositById(Long rdId) {
        return recurringDepositRepository.findById(rdId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring deposit not found with id: " + rdId));
    }

    @Transactional(readOnly = true)
    public List<RdTransaction> getInstallments(Long rdId) {
        return rdTransactionRepository.findByRecurringDepositRdIdOrderByInstallmentNoAsc(rdId);
    }

    @Transactional(readOnly = true)
    public List<RecurringDeposit> getDepositsByMember(Long memberId) {
        return recurringDepositRepository.findByMemberMemberId(memberId);
    }
}
