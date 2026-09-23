package com.ebank.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummary {
    private long totalMembers;
    private long activeMembers;
    private long totalActiveLoans;
    private BigDecimal totalDeposits;
    private BigDecimal totalLoanOutstanding;
    private BigDecimal todayTransactions;
    private long todayTransactionCount;
    private BigDecimal monthlyDeposits;
    private BigDecimal monthlyLoanRecovery;
    private long overdueEmiCount;
    private long maturingFdCount;
    private long pendingApprovals;
    private BigDecimal totalShareCapital;
}