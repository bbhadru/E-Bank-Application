package com.ebank.service;

import com.ebank.dto.MemberRequest;
import com.ebank.dto.MemberResponse;
import com.ebank.exception.DuplicateEntryException;
import com.ebank.exception.ResourceNotFoundException;
import com.ebank.model.Member;
import com.ebank.model.SavingsAccount;
import com.ebank.repository.MemberRepository;
import com.ebank.repository.SavingsAccountRepository;
import com.ebank.util.AccountNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AuditService auditService;

    public MemberService(MemberRepository memberRepository,
                         SavingsAccountRepository savingsAccountRepository,
                         AccountNumberGenerator accountNumberGenerator,
                         AuditService auditService) {
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.auditService = auditService;
    }

    @Transactional
    public Member createMember(MemberRequest request) {
        if (memberRepository.existsByMobile(request.getMobile())) {
            throw new DuplicateEntryException("Member already exists with mobile: " + request.getMobile());
        }
        if (request.getKycNumber() != null && !request.getKycNumber().isBlank()
                && memberRepository.existsByKycNumber(request.getKycNumber())) {
            throw new DuplicateEntryException("Member already exists with KYC number: " + request.getKycNumber());
        }

        Long maxId = memberRepository.findMaxId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        String memberCode = accountNumberGenerator.generateMemberCode(nextId);

        Member member = Member.builder()
                .memberCode(memberCode)
                .fullName(request.getFullName())
                .dob(request.getDob())
                .gender(request.getGender())
                .mobile(request.getMobile())
                .email(request.getEmail())
                .address(request.getAddress())
                .kycNumber(request.getKycNumber())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Member savedMember = memberRepository.save(member);
        auditService.log(null, "MEMBER_CREATED", "Member", savedMember.getMemberId(), null, savedMember.getMemberCode());

        // Optional auto-creation of Savings Account
        if (request.isCreateSavingsAccount()) {
            Long sbMaxId = savingsAccountRepository.findMaxId();
            long nextSbId = (sbMaxId != null ? sbMaxId : 0) + 1;
            String sbNumber = accountNumberGenerator.generateSbAccountNumber(nextSbId);

            SavingsAccount account = SavingsAccount.builder()
                    .accountNumber(sbNumber)
                    .member(savedMember)
                    .balance(BigDecimal.ZERO)
                    .interestRate(new BigDecimal("4.00"))
                    .minimumBalance(new BigDecimal("500.00"))
                    .status("ACTIVE")
                    .openedDate(LocalDate.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            savingsAccountRepository.save(account);
            auditService.log(null, "SAVINGS_ACCOUNT_AUTOCREATED", "SavingsAccount", account.getSbId(), null, sbNumber);
        }

        return savedMember;
    }

    @Transactional
    public Member updateMember(Long memberId, MemberRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));

        member.setFullName(request.getFullName());
        member.setDob(request.getDob());
        member.setGender(request.getGender());
        member.setMobile(request.getMobile());
        member.setEmail(request.getEmail());
        member.setAddress(request.getAddress());
        member.setKycNumber(request.getKycNumber());
        member.setUpdatedAt(LocalDateTime.now());

        Member updated = memberRepository.save(member);
        auditService.log(null, "MEMBER_UPDATED", "Member", memberId, null, "Updated profile");
        return updated;
    }

    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));
        member.setStatus("INACTIVE");
        memberRepository.save(member);
        auditService.log(null, "MEMBER_DEACTIVATED", "Member", memberId, "ACTIVE", "INACTIVE");
    }

    @Transactional(readOnly = true)
    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));
    }

    @Transactional(readOnly = true)
    public Member getMemberByCode(String code) {
        return memberRepository.findByMemberCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with code: " + code));
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> searchMembers(String query, String status, Pageable pageable) {
        return memberRepository.searchMembers(query, status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getAllMembersList(String query) {
        return memberRepository.searchMembersList(query).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MemberResponse mapToResponse(Member member) {
        return MemberResponse.builder()
                .memberId(member.getMemberId())
                .memberCode(member.getMemberCode())
                .fullName(member.getFullName())
                .dob(member.getDob())
                .gender(member.getGender())
                .mobile(member.getMobile())
                .email(member.getEmail())
                .address(member.getAddress())
                .kycNumber(member.getKycNumber())
                .status(member.getStatus())
                .createdAt(member.getCreatedAt())
                .savingsAccountsCount(member.getSavingsAccounts() != null ? member.getSavingsAccounts().size() : 0)
                .loansCount(member.getLoans() != null ? member.getLoans().size() : 0)
                .build();
    }
}
