package com.ebank.service;

import com.ebank.dto.ShareCapitalRequest;
import com.ebank.dto.TransactionRequest;
import com.ebank.exception.InvalidOperationException;
import com.ebank.exception.ResourceNotFoundException;
import com.ebank.model.Member;
import com.ebank.model.SavingsAccount;
import com.ebank.model.ShareCapital;
import com.ebank.repository.MemberRepository;
import com.ebank.repository.ShareCapitalRepository;
import com.ebank.util.AccountNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShareCapitalService {

    private final ShareCapitalRepository shareCapitalRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountService savingsAccountService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final VoucherService voucherService;
    private final AuditService auditService;

    public ShareCapitalService(ShareCapitalRepository shareCapitalRepository,
                               MemberRepository memberRepository,
                               SavingsAccountService savingsAccountService,
                               AccountNumberGenerator accountNumberGenerator,
                               VoucherService voucherService,
                               AuditService auditService) {
        this.shareCapitalRepository = shareCapitalRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountService = savingsAccountService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.voucherService = voucherService;
        this.auditService = auditService;
    }

    @Transactional
    public ShareCapital allotShares(ShareCapitalRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + request.getMemberId()));

        Long maxId = shareCapitalRepository.findMaxId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        String certificateNo = accountNumberGenerator.generateCertificateNumber(nextId);

        BigDecimal shareFaceValue = new BigDecimal("100.00");
        BigDecimal totalAmount = shareFaceValue.multiply(BigDecimal.valueOf(request.getSharesCount()));

        ShareCapital shareCapital = ShareCapital.builder()
                .member(member)
                .certificateNumber(certificateNo)
                .sharesCount(request.getSharesCount())
                .shareValue(shareFaceValue)
                .totalAmount(totalAmount)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ShareCapital saved = shareCapitalRepository.save(shareCapital);

        // Auto post double-entry voucher: Debit Cash, Credit Share Capital
        voucherService.postAutomatedVoucher(
                "RECEIPT",
                "Share Capital Allotment: " + certificateNo + " (" + request.getSharesCount() + " shares)",
                "CASH_ACCOUNT", totalAmount,
                "SHARE_CAPITAL_LIABILITY", totalAmount
        );

        auditService.log(null, "SHARES_ALLOTTED", "ShareCapital", saved.getShareId(), null, certificateNo);
        return saved;
    }

    @Transactional
    public void distributeDividend(BigDecimal dividendPerShare) {
        if (dividendPerShare == null || dividendPerShare.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Dividend per share must be positive");
        }

        List<ShareCapital> allShares = shareCapitalRepository.findAll();
        for (ShareCapital sc : allShares) {
            if ("ACTIVE".equalsIgnoreCase(sc.getStatus()) && sc.getSharesCount() > 0) {
                BigDecimal payout = dividendPerShare.multiply(BigDecimal.valueOf(sc.getSharesCount()));
                Member member = sc.getMember();
                List<SavingsAccount> accounts = member.getSavingsAccounts();
                if (accounts != null && !accounts.isEmpty()) {
                    SavingsAccount target = accounts.get(0);
                    savingsAccountService.deposit(TransactionRequest.builder()
                            .accountId(target.getSbId())
                            .amount(payout)
                            .narration("Dividend Credit for " + sc.getCertificateNumber())
                            .build());
                }

                voucherService.postAutomatedVoucher(
                        "PAYMENT",
                        "Dividend distribution for Certificate: " + sc.getCertificateNumber(),
                        "DIVIDEND_EXPENSE", payout,
                        "MEMBER_SAVINGS_DEPOSITS", payout
                );
            }
        }
        auditService.log(null, "DIVIDEND_DISTRIBUTED", "ShareCapital", null, null, "Rate: " + dividendPerShare);
    }

    @Transactional(readOnly = true)
    public Page<ShareCapital> searchShares(String query, Pageable pageable) {
        return shareCapitalRepository.searchShares(query, pageable);
    }

    @Transactional(readOnly = true)
    public List<ShareCapital> getSharesByMember(Long memberId) {
        return shareCapitalRepository.findByMemberMemberId(memberId);
    }
}
