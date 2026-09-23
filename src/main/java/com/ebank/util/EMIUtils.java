package com.ebank.util;

import com.ebank.model.Loan;
import com.ebank.model.LoanSchedule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EMIUtils {

    public static BigDecimal calculateReducingEMI(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (principal == null || annualRate == null || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        double p = principal.doubleValue();
        double r = annualRate.doubleValue() / (12.0 * 100.0);
        int n = tenureMonths;

        double emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateFlatEMI(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (principal == null || annualRate == null || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal years = BigDecimal.valueOf(tenureMonths).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal totalInterest = principal.multiply(annualRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).multiply(years);
        BigDecimal totalPayable = principal.add(totalInterest);
        return totalPayable.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
    }

    public static List<LoanSchedule> generateSchedule(Loan loan) {
        List<LoanSchedule> schedules = new ArrayList<>();
        BigDecimal principal = loan.getPrincipal();
        BigDecimal annualRate = loan.getInterestRate();
        int tenureMonths = loan.getTenureMonths();
        LocalDate startDate = loan.getStartDate() != null ? loan.getStartDate() : LocalDate.now();
        String emiType = loan.getEmiType() != null ? loan.getEmiType() : "REDUCING";

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 8, RoundingMode.HALF_UP);
        BigDecimal emi = loan.getEmiAmount();
        if (emi == null || emi.compareTo(BigDecimal.ZERO) == 0) {
            emi = "FLAT".equalsIgnoreCase(emiType) ? 
                    calculateFlatEMI(principal, annualRate, tenureMonths) : 
                    calculateReducingEMI(principal, annualRate, tenureMonths);
        }

        BigDecimal remainingBalance = principal;

        for (int i = 1; i <= tenureMonths; i++) {
            LocalDate dueDate = startDate.plusMonths(i);
            BigDecimal interestComponent;
            BigDecimal principalComponent;

            if ("FLAT".equalsIgnoreCase(emiType)) {
                BigDecimal totalFlatInterest = principal.multiply(annualRate)
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(tenureMonths).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP));
                interestComponent = totalFlatInterest.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
                principalComponent = emi.subtract(interestComponent);
            } else {
                interestComponent = remainingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
                if (i == tenureMonths) {
                    principalComponent = remainingBalance;
                    emi = principalComponent.add(interestComponent);
                } else {
                    principalComponent = emi.subtract(interestComponent);
                }
            }

            remainingBalance = remainingBalance.subtract(principalComponent);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }

            LoanSchedule schedule = LoanSchedule.builder()
                    .loan(loan)
                    .installmentNo(i)
                    .dueDate(dueDate)
                    .emiAmount(emi)
                    .principalComponent(principalComponent)
                    .interestComponent(interestComponent)
                    .balanceAfter(remainingBalance)
                    .paidStatus(false)
                    .penaltyAmount(BigDecimal.ZERO)
                    .build();

            schedules.add(schedule);
        }

        return schedules;
    }
}
