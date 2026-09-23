package com.ebank.service;

import com.ebank.dto.ReportFilter;
import com.ebank.model.*;
import com.ebank.repository.*;
import com.ebank.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ReportService {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SbTransactionRepository sbTransactionRepository;
    private final FixedDepositRepository fixedDepositRepository;
    private final RecurringDepositRepository recurringDepositRepository;
    private final LoanRepository loanRepository;
    private final ShareCapitalRepository shareCapitalRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public ReportService(MemberRepository memberRepository,
                         SavingsAccountRepository savingsAccountRepository,
                         SbTransactionRepository sbTransactionRepository,
                         FixedDepositRepository fixedDepositRepository,
                         RecurringDepositRepository recurringDepositRepository,
                         LoanRepository loanRepository,
                         ShareCapitalRepository shareCapitalRepository,
                         LedgerAccountRepository ledgerAccountRepository,
                         LedgerEntryRepository ledgerEntryRepository) {
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.sbTransactionRepository = sbTransactionRepository;
        this.fixedDepositRepository = fixedDepositRepository;
        this.recurringDepositRepository = recurringDepositRepository;
        this.loanRepository = loanRepository;
        this.shareCapitalRepository = shareCapitalRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateTrialBalance() {
        List<LedgerAccount> accounts = ledgerAccountRepository.findAll();
        List<Map<String, Object>> entries = new ArrayList<>();

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (LedgerAccount acc : accounts) {
            BigDecimal debitSum = ledgerEntryRepository.sumDebitByLedgerId(acc.getLedgerId());
            BigDecimal creditSum = ledgerEntryRepository.sumCreditByLedgerId(acc.getLedgerId());

            if (debitSum.compareTo(BigDecimal.ZERO) == 0 && creditSum.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Map<String, Object> row = new HashMap<>();
            row.put("code", acc.getLedgerCode());
            row.put("name", acc.getLedgerName());
            row.put("type", acc.getLedgerType());
            row.put("debit", debitSum);
            row.put("credit", creditSum);

            totalDebit = totalDebit.add(debitSum);
            totalCredit = totalCredit.add(creditSum);
            entries.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("entries", entries);
        result.put("totalDebit", totalDebit);
        result.put("totalCredit", totalCredit);
        result.put("isBalanced", totalDebit.compareTo(totalCredit) == 0);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateProfitAndLoss() {
        List<LedgerAccount> accounts = ledgerAccountRepository.findAll();
        List<Map<String, Object>> incomes = new ArrayList<>();
        List<Map<String, Object>> expenses = new ArrayList<>();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (LedgerAccount acc : accounts) {
            if ("INCOME".equalsIgnoreCase(acc.getLedgerType())) {
                BigDecimal amount = acc.getCurrentBalance();
                if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", acc.getLedgerName());
                    row.put("amount", amount);
                    incomes.add(row);
                    totalIncome = totalIncome.add(amount);
                }
            } else if ("EXPENSE".equalsIgnoreCase(acc.getLedgerType())) {
                BigDecimal amount = acc.getCurrentBalance();
                if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", acc.getLedgerName());
                    row.put("amount", amount);
                    expenses.add(row);
                    totalExpense = totalExpense.add(amount);
                }
            }
        }

        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        Map<String, Object> result = new HashMap<>();
        result.put("incomes", incomes);
        result.put("expenses", expenses);
        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("netProfit", netProfit);
        return result;
    }

    @Transactional(readOnly = true)
    public List<List<String>> getReportData(String reportType, ReportFilter filter) {
        List<List<String>> rows = new ArrayList<>();

        if ("MEMBERS".equalsIgnoreCase(reportType)) {
            List<Member> members = memberRepository.findAll();
            for (Member m : members) {
                rows.add(Arrays.asList(
                        m.getMemberCode(),
                        m.getFullName(),
                        m.getMobile(),
                        m.getEmail() != null ? m.getEmail() : "-",
                        m.getStatus(),
                        DateUtils.formatDateTime(m.getCreatedAt())
                ));
            }
        } else if ("SAVINGS".equalsIgnoreCase(reportType)) {
            List<SavingsAccount> accounts = savingsAccountRepository.findAll();
            for (SavingsAccount s : accounts) {
                rows.add(Arrays.asList(
                        s.getAccountNumber(),
                        s.getMember().getFullName(),
                        s.getMember().getMemberCode(),
                        s.getBalance().toString(),
                        s.getInterestRate() + "%",
                        s.getStatus(),
                        DateUtils.formatDate(s.getOpenedDate())
                ));
            }
        } else if ("FD".equalsIgnoreCase(reportType)) {
            List<FixedDeposit> fds = fixedDepositRepository.findAll();
            for (FixedDeposit fd : fds) {
                rows.add(Arrays.asList(
                        fd.getFdNumber(),
                        fd.getMember().getFullName(),
                        fd.getPrincipal().toString(),
                        fd.getInterestRate() + "%",
                        fd.getTenureMonths() + " M",
                        fd.getMaturityAmount().toString(),
                        DateUtils.formatDate(fd.getMaturityDate()),
                        fd.getStatus()
                ));
            }
        } else if ("RD".equalsIgnoreCase(reportType)) {
            List<RecurringDeposit> rds = recurringDepositRepository.findAll();
            for (RecurringDeposit rd : rds) {
                rows.add(Arrays.asList(
                        rd.getRdNumber(),
                        rd.getMember().getFullName(),
                        rd.getMonthlyAmount().toString(),
                        rd.getInterestRate() + "%",
                        rd.getTenureMonths() + " M",
                        rd.getMaturityAmount().toString(),
                        DateUtils.formatDate(rd.getMaturityDate()),
                        rd.getStatus()
                ));
            }
        } else if ("LOANS".equalsIgnoreCase(reportType)) {
            List<Loan> loans = loanRepository.findAll();
            for (Loan l : loans) {
                rows.add(Arrays.asList(
                        l.getLoanNumber(),
                        l.getMember().getFullName(),
                        l.getLoanType(),
                        l.getPrincipal().toString(),
                        l.getOutstandingBalance().toString(),
                        l.getEmiAmount().toString(),
                        Boolean.TRUE.equals(l.getNpaFlag()) ? "YES (NPA)" : "NO",
                        l.getStatus()
                ));
            }
        } else if ("TRIAL_BALANCE".equalsIgnoreCase(reportType)) {
            Map<String, Object> tb = generateTrialBalance();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) tb.get("entries");
            for (Map<String, Object> entry : entries) {
                rows.add(Arrays.asList(
                        entry.get("code").toString(),
                        entry.get("name").toString(),
                        entry.get("type").toString(),
                        entry.get("debit").toString(),
                        entry.get("credit").toString()
                ));
            }
        }

        return rows;
    }

    public List<String> getReportHeaders(String reportType) {
        if ("MEMBERS".equalsIgnoreCase(reportType)) {
            return Arrays.asList("Member Code", "Full Name", "Mobile", "Email", "Status", "Joined Date");
        } else if ("SAVINGS".equalsIgnoreCase(reportType)) {
            return Arrays.asList("Account No", "Member Name", "Member Code", "Balance (INR)", "Interest Rate", "Status", "Opened Date");
        } else if ("FD".equalsIgnoreCase(reportType)) {
            return Arrays.asList("FD Number", "Member Name", "Principal (INR)", "Interest Rate", "Tenure", "Maturity Amount", "Maturity Date", "Status");
        } else if ("RD".equalsIgnoreCase(reportType)) {
            return Arrays.asList("RD Number", "Member Name", "Monthly Amount", "Interest Rate", "Tenure", "Maturity Amount", "Maturity Date", "Status");
        } else if ("LOANS".equalsIgnoreCase(reportType)) {
            return Arrays.asList("Loan No", "Member Name", "Loan Type", "Principal", "Outstanding Balance", "EMI", "NPA Flag", "Status");
        } else if ("TRIAL_BALANCE".equalsIgnoreCase(reportType)) {
            return Arrays.asList("Ledger Code", "Ledger Name", "Type", "Debit Total (INR)", "Credit Total (INR)");
        }
        return Collections.emptyList();
    }
}
