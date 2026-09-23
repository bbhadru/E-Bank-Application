package com.ebank.service;

import com.ebank.dto.SavingsAccountRequest;
import com.ebank.dto.TransactionRequest;
import com.ebank.exception.InsufficientBalanceException;
import com.ebank.model.Member;
import com.ebank.model.SavingsAccount;
import com.ebank.model.SbTransaction;
import com.ebank.repository.MemberRepository;
import com.ebank.repository.SavingsAccountRepository;
import com.ebank.repository.SbTransactionRepository;
import com.ebank.util.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SavingsAccountServiceTest {

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Mock
    private SbTransactionRepository sbTransactionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private VoucherService voucherService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SavingsAccountService savingsAccountService;

    private Member sampleMember;
    private SavingsAccount sampleAccount;

    @BeforeEach
    void setUp() {
        sampleMember = Member.builder()
                .memberId(1L)
                .memberCode("MEM-00001")
                .fullName("Test User")
                .mobile("9876543210")
                .build();

        sampleAccount = SavingsAccount.builder()
                .sbId(1L)
                .accountNumber("SB10000001")
                .member(sampleMember)
                .balance(new BigDecimal("5000.00"))
                .minimumBalance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .openedDate(LocalDate.now())
                .build();
    }

    @Test
    void testCreateAccount() {
        SavingsAccountRequest request = SavingsAccountRequest.builder()
                .memberId(1L)
                .interestRate(new BigDecimal("4.00"))
                .minimumBalance(new BigDecimal("500.00"))
                .build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(accountNumberGenerator.generateSbAccountNumber(any())).thenReturn("SB10000001");
        when(savingsAccountRepository.save(any(SavingsAccount.class))).thenReturn(sampleAccount);

        SavingsAccount result = savingsAccountService.createAccount(request);

        assertNotNull(result);
        assertEquals("SB10000001", result.getAccountNumber());
        verify(savingsAccountRepository, times(1)).save(any(SavingsAccount.class));
    }

    @Test
    void testDeposit() {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(1L)
                .amount(new BigDecimal("1000.00"))
                .narration("Cash Deposit")
                .build();

        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(sbTransactionRepository.save(any(SbTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SbTransaction result = savingsAccountService.deposit(request);

        assertNotNull(result);
        assertEquals("DEPOSIT", result.getTxnType());
        assertEquals(new BigDecimal("6000.00"), sampleAccount.getBalance());
        verify(voucherService, times(1)).postAutomatedVoucher(eq("RECEIPT"), anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void testWithdraw_Success() {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(1L)
                .amount(new BigDecimal("2000.00"))
                .narration("ATM Withdrawal")
                .build();

        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(sbTransactionRepository.save(any(SbTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SbTransaction result = savingsAccountService.withdraw(request);

        assertNotNull(result);
        assertEquals("WITHDRAW", result.getTxnType());
        assertEquals(new BigDecimal("3000.00"), sampleAccount.getBalance());
        verify(voucherService, times(1)).postAutomatedVoucher(eq("PAYMENT"), anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void testWithdraw_InsufficientBalance() {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(1L)
                .amount(new BigDecimal("4800.00")) // Remaining would be 200, below 500 minimum
                .build();

        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        assertThrows(InsufficientBalanceException.class, () -> savingsAccountService.withdraw(request));
        verify(sbTransactionRepository, never()).save(any());
    }
}
