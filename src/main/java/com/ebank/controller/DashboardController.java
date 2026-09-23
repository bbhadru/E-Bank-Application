package com.ebank.controller;

import com.ebank.dto.DashboardSummary;
import com.ebank.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Controller
public class DashboardController {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SbTransactionRepository sbTransactionRepository;
    private final FixedDepositRepository fixedDepositRepository;
    private final LoanRepository loanRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final ShareCapitalRepository shareCapitalRepository;

    public DashboardController(MemberRepository memberRepository,
                               SavingsAccountRepository savingsAccountRepository,
                               SbTransactionRepository sbTransactionRepository,
                               FixedDepositRepository fixedDepositRepository,
                               LoanRepository loanRepository,
                               LoanScheduleRepository loanScheduleRepository,
                               LoanTransactionRepository loanTransactionRepository,
                               ShareCapitalRepository shareCapitalRepository) {
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.sbTransactionRepository = sbTransactionRepository;
        this.fixedDepositRepository = fixedDepositRepository;
        this.loanRepository = loanRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.loanTransactionRepository = loanTransactionRepository;
        this.shareCapitalRepository = shareCapitalRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        DashboardSummary summary = getSummaryData();
        model.addAttribute("summary", summary);
        return "dashboard/index";
    }

    @GetMapping("/api/dashboard/summary")
    @ResponseBody
    public ResponseEntity<DashboardSummary> getSummaryApi() {
        return ResponseEntity.ok(getSummaryData());
    }

    @GetMapping("/api/dashboard/chart-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChartDataApi() {
        List<String> months = Arrays.asList("May", "Jun", "Jul", "Aug", "Sep", "Oct");
        List<BigDecimal> deposits = Arrays.asList(
                new BigDecimal("45000"), new BigDecimal("58000"), new BigDecimal("62000"),
                new BigDecimal("75000"), new BigDecimal("82000"), new BigDecimal("96000")
        );
        List<BigDecimal> loanRecovery = Arrays.asList(
                new BigDecimal("32000"), new BigDecimal("38000"), new BigDecimal("44000"),
                new BigDecimal("51000"), new BigDecimal("59000"), new BigDecimal("67000")
        );

        Map<String, Object> data = new HashMap<>();
        data.put("labels", months);
        data.put("deposits", deposits);
        data.put("loanRecovery", loanRecovery);
        return ResponseEntity.ok(data);
    }

    private DashboardSummary getSummaryData() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        long totalMembers = memberRepository.count();
        long activeMembers = memberRepository.countByStatus("ACTIVE");
        long totalActiveLoans = loanRepository.countByStatus("ACTIVE") + loanRepository.countByStatus("DISBURSED");

        BigDecimal sbDeposits = savingsAccountRepository.sumActiveBalances();
        BigDecimal fdDeposits = fixedDepositRepository.sumActivePrincipal();
        BigDecimal totalDeposits = (sbDeposits != null ? sbDeposits : BigDecimal.ZERO)
                .add(fdDeposits != null ? fdDeposits : BigDecimal.ZERO);

        BigDecimal loanOutstanding = loanRepository.sumActiveOutstanding();

        BigDecimal todayTxnAmount = sbTransactionRepository.sumAmountByTxnDateBetween(startOfDay, endOfDay);
        long todayTxnCount = sbTransactionRepository.countByTxnDateBetween(startOfDay, endOfDay);

        BigDecimal monthlyDeposits = sbTransactionRepository.sumDepositsBetween(startOfMonth, endOfDay);
        BigDecimal monthlyLoanRecovery = loanTransactionRepository.sumAmountByPaymentDateBetween(today.withDayOfMonth(1), today);

        long overdueEmis = loanScheduleRepository.countOverdueSchedules(today);
        long maturingFds = fixedDepositRepository.countMaturingDeposits(today.plusDays(30));
        long pendingApprovals = loanRepository.countByStatus("PENDING");
        BigDecimal totalShareCapital = shareCapitalRepository.sumTotalCapital();

        return DashboardSummary.builder()
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .totalActiveLoans(totalActiveLoans)
                .totalDeposits(totalDeposits != null ? totalDeposits : BigDecimal.ZERO)
                .totalLoanOutstanding(loanOutstanding != null ? loanOutstanding : BigDecimal.ZERO)
                .todayTransactions(todayTxnAmount != null ? todayTxnAmount : BigDecimal.ZERO)
                .todayTransactionCount(todayTxnCount)
                .monthlyDeposits(monthlyDeposits != null ? monthlyDeposits : BigDecimal.ZERO)
                .monthlyLoanRecovery(monthlyLoanRecovery != null ? monthlyLoanRecovery : BigDecimal.ZERO)
                .overdueEmiCount(overdueEmis)
                .maturingFdCount(maturingFds)
                .pendingApprovals(pendingApprovals)
                .totalShareCapital(totalShareCapital != null ? totalShareCapital : BigDecimal.ZERO)
                .build();
    }
}
