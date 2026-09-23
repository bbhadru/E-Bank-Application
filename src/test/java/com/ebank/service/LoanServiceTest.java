package com.ebank.service;

import com.ebank.dto.LoanRepaymentRequest;
import com.ebank.dto.LoanRequest;
import com.ebank.model.Loan;
import com.ebank.model.LoanSchedule;
import com.ebank.model.LoanTransaction;
import com.ebank.model.Member;
import com.ebank.repository.LoanRepository;
import com.ebank.repository.LoanScheduleRepository;
import com.ebank.repository.LoanTransactionRepository;
import com.ebank.repository.MemberRepository;
import com.ebank.util.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanScheduleRepository loanScheduleRepository;

    @Mock
    private LoanTransactionRepository loanTransactionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SavingsAccountService savingsAccountService;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private VoucherService voucherService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private LoanService loanService;

    private Member sampleMember;
    private Loan sampleLoan;

    @BeforeEach
    void setUp() {
        sampleMember = Member.builder()
                .memberId(1L)
                .memberCode("MEM-00001")
                .fullName("Jane Doe")
                .build();

        sampleLoan = Loan.builder()
                .loanId(1L)
                .loanNumber("LN40000001")
                .member(sampleMember)
                .loanType("Personal Loan")
                .principal(new BigDecimal("12000.00"))
                .interestRate(new BigDecimal("12.00"))
                .tenureMonths(12)
                .emiAmount(new BigDecimal("1066.00"))
                .outstandingBalance(new BigDecimal("12000.00"))
                .status("PENDING")
                .emiType("REDUCING")
                .build();
    }

    @Test
    void testApplyLoan() {
        LoanRequest request = LoanRequest.builder()
                .memberId(1L)
                .loanType("Personal Loan")
                .principal(new BigDecimal("12000.00"))
                .interestRate(new BigDecimal("12.00"))
                .tenureMonths(12)
                .emiType("REDUCING")
                .build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(accountNumberGenerator.generateLoanNumber(any())).thenReturn("LN40000001");
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.applyLoan(request);

        assertNotNull(result);
        assertEquals("LN40000001", result.getLoanNumber());
        assertEquals("PENDING", result.getStatus());
        assertTrue(result.getEmiAmount().compareTo(BigDecimal.ZERO) > 0);
        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    void testApproveLoan() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(sampleLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan approved = loanService.approveLoan(1L, 99L);

        assertEquals("APPROVED", approved.getStatus());
        assertEquals(99L, approved.getApprovedBy());
    }

    @Test
    void testDisburseLoan() {
        sampleLoan.setStatus("APPROVED");
        when(loanRepository.findById(1L)).thenReturn(Optional.of(sampleLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan disbursed = loanService.disburseLoan(1L, null);

        assertEquals("ACTIVE", disbursed.getStatus());
        assertNotNull(disbursed.getStartDate());
        verify(loanScheduleRepository, times(1)).saveAll(anyList());
        verify(voucherService, times(1)).postAutomatedVoucher(eq("PAYMENT"), anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void testProcessRepayment() {
        sampleLoan.setStatus("ACTIVE");

        LoanSchedule schedule = LoanSchedule.builder()
                .scheduleId(1L)
                .loan(sampleLoan)
                .installmentNo(1)
                .dueDate(LocalDate.now())
                .emiAmount(new BigDecimal("1066.00"))
                .interestComponent(new BigDecimal("120.00"))
                .principalComponent(new BigDecimal("946.00"))
                .paidStatus(false)
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(sampleLoan));
        when(loanScheduleRepository.findByLoanLoanIdAndPaidStatusFalseOrderByInstallmentNoAsc(1L))
                .thenReturn(Collections.singletonList(schedule));
        when(loanTransactionRepository.save(any(LoanTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanRepaymentRequest request = LoanRepaymentRequest.builder()
                .loanId(1L)
                .amount(new BigDecimal("1066.00"))
                .paymentType("EMI")
                .build();

        LoanTransaction txn = loanService.processRepayment(request);

        assertNotNull(txn);
        assertEquals(new BigDecimal("1066.00"), txn.getAmount());
        assertTrue(schedule.getPaidStatus());
        verify(voucherService, atLeastOnce()).postAutomatedVoucher(eq("RECEIPT"), anyString(), anyString(), any(), anyString(), any());
    }
}
