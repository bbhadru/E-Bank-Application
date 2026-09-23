package com.ebank.service;

import com.ebank.dto.FixedDepositRequest;
import com.ebank.dto.TransactionRequest;
import com.ebank.exception.InvalidOperationException;
import com.ebank.exception.ResourceNotFoundException;
import com.ebank.model.FixedDeposit;
import com.ebank.model.Member;
import com.ebank.model.SavingsAccount;
import com.ebank.repository.FixedDepositRepository;
import com.ebank.repository.MemberRepository;
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
import java.util.List;

@Service
public class FixedDepositService {

    private final FixedDepositRepository fixedDepositRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsAccountService savingsAccountService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final VoucherService voucherService;
    private final AuditService auditService;

    public FixedDepositService(FixedDepositRepository fixedDepositRepository,
                               MemberRepository memberRepository,
                               SavingsAccountRepository savingsAccountRepository,
                               SavingsAccountService savingsAccountService,
                               AccountNumberGenerator accountNumberGenerator,
                               VoucherService voucherService,
                               AuditService auditService) {
        this.fixedDepositRepository = fixedDepositRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsAccountService = savingsAccountService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.voucherService = voucherService;
        this.auditService = auditService;
    }

    @Transactional
    public FixedDeposit createFixedDeposit(FixedDepositRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + request.getMemberId()));

        SavingsAccount linkedSb = null;
        if (request.getLinkedSbId() != null) {
            linkedSb = savingsAccountRepository.findById(request.getLinkedSbId())
                    .orElse(null);
            // Optionally debit linked SB account
            if (linkedSb != null && linkedSb.isActive()) {
                savingsAccountService.withdraw(TransactionRequest.builder()
                        .accountId(linkedSb.getSbId())
                        .amount(request.getPrincipal())
                        .narration("Debit for Fixed Deposit opening")
                        .build());
            }
        }

        Long maxId = fixedDepositRepository.findMaxId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        String fdNumber = accountNumberGenerator.generateFdNumber(nextId);

        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(request.getTenureMonths());

        BigDecimal maturityAmount = "COMPOUND".equalsIgnoreCase(request.getInterestType()) ?
                InterestCalculator.calculateFdCompoundInterestMaturity(request.getPrincipal(), request.getInterestRate(), request.getTenureMonths(), 4) :
                InterestCalculator.calculateFdSimpleInterestMaturity(request.getPrincipal(), request.getInterestRate(), request.getTenureMonths());

        FixedDeposit fd = FixedDeposit.builder()
                .fdNumber(fdNumber)
                .member(member)
                .linkedSavingsAccount(linkedSb)
                .principal(request.getPrincipal())
                .interestRate(request.getInterestRate())
                .tenureMonths(request.getTenureMonths())
                .maturityAmount(maturityAmount)
                .interestType(request.getInterestType() != null ? request.getInterestType() : "SIMPLE")
                .startDate(startDate)
                .maturityDate(maturityDate)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        FixedDeposit saved = fixedDepositRepository.save(fd);

        // Auto post double-entry accounting voucher: Debit Cash (or Member SB), Credit FD Liability
        String debitAccount = (linkedSb != null) ? "MEMBER_SAVINGS_DEPOSITS" : "CASH_ACCOUNT";
        voucherService.postAutomatedVoucher(
                "RECEIPT",
                "Fixed Deposit Created: " + fdNumber,
                debitAccount, request.getPrincipal(),
                "FIXED_DEPOSIT_LIABILITIES", request.getPrincipal()
        );

        auditService.log(null, "FD_CREATED", "FixedDeposit", saved.getFdId(), null, fdNumber);
        return saved;
    }

    @Transactional
    public void matureOrCloseDeposit(Long fdId) {
        FixedDeposit fd = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed deposit not found with id: " + fdId));

        if (!fd.isActive()) {
            throw new InvalidOperationException("Fixed deposit is already closed/matured");
        }

        fd.setStatus("MATURED");
        fd.setClosedDate(LocalDate.now());
        fixedDepositRepository.save(fd);

        BigDecimal interestEarned = fd.getMaturityAmount().subtract(fd.getPrincipal());
        if (interestEarned.compareTo(BigDecimal.ZERO) < 0) interestEarned = BigDecimal.ZERO;

        // Auto transfer to linked SB account if present
        if (fd.getLinkedSavingsAccount() != null && fd.getLinkedSavingsAccount().isActive()) {
            savingsAccountService.deposit(TransactionRequest.builder()
                    .accountId(fd.getLinkedSavingsAccount().getSbId())
                    .amount(fd.getMaturityAmount())
                    .narration("Fixed Deposit Maturity Payout: " + fd.getFdNumber())
                    .build());
        }

        // Auto post voucher: Debit FD Liability + Debit Interest Expense, Credit Cash / SB
        String creditAccount = (fd.getLinkedSavingsAccount() != null) ? "MEMBER_SAVINGS_DEPOSITS" : "CASH_ACCOUNT";
        voucherService.postAutomatedVoucher(
                "PAYMENT",
                "FD Principal Payout: " + fd.getFdNumber(),
                "FIXED_DEPOSIT_LIABILITIES", fd.getPrincipal(),
                creditAccount, fd.getPrincipal()
        );

        if (interestEarned.compareTo(BigDecimal.ZERO) > 0) {
            voucherService.postAutomatedVoucher(
                    "JOURNAL",
                    "FD Interest Payout: " + fd.getFdNumber(),
                    "INTEREST_EXPENSE_FD", interestEarned,
                    creditAccount, interestEarned
            );
        }

        auditService.log(null, "FD_MATURED", "FixedDeposit", fdId, "ACTIVE", "MATURED");
    }

    @Transactional(readOnly = true)
    public Page<FixedDeposit> searchDeposits(String query, String status, Pageable pageable) {
        return fixedDepositRepository.searchDeposits(query, status, pageable);
    }

    @Transactional(readOnly = true)
    public FixedDeposit getDepositById(Long fdId) {
        return fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed deposit not found with id: " + fdId));
    }

    @Transactional(readOnly = true)
    public List<FixedDeposit> getDepositsByMember(Long memberId) {
        return fixedDepositRepository.findByMemberMemberId(memberId);
    }
}
