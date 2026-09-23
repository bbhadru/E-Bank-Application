package com.ebank.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InterestCalculator {

    public static BigDecimal calculateDailySbInterest(BigDecimal balance, BigDecimal annualRate) {
        if (balance == null || annualRate == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return balance.multiply(annualRate)
                .divide(BigDecimal.valueOf(36500), 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateFdSimpleInterestMaturity(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (principal == null || annualRate == null || tenureMonths <= 0) {
            return principal != null ? principal : BigDecimal.ZERO;
        }
        BigDecimal interest = principal.multiply(annualRate).multiply(BigDecimal.valueOf(tenureMonths))
                .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP);
        return principal.add(interest);
    }

    public static BigDecimal calculateFdCompoundInterestMaturity(BigDecimal principal, BigDecimal annualRate, int tenureMonths, int compoundingFreqPerYear) {
        if (principal == null || annualRate == null || tenureMonths <= 0) {
            return principal != null ? principal : BigDecimal.ZERO;
        }
        double p = principal.doubleValue();
        double r = annualRate.doubleValue() / 100.0;
        int n = compoundingFreqPerYear > 0 ? compoundingFreqPerYear : 4; // Quarterly default
        double t = tenureMonths / 12.0;

        double amount = p * Math.pow(1 + (r / n), n * t);
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateRdMaturityAmount(BigDecimal monthlyInstallment, BigDecimal annualRate, int tenureMonths) {
        if (monthlyInstallment == null || annualRate == null || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }
        double p = monthlyInstallment.doubleValue();
        double r = annualRate.doubleValue() / (12.0 * 100.0);
        int n = tenureMonths;

        double totalMaturity = 0;
        for (int i = 1; i <= n; i++) {
            int remainingMonths = n - i + 1;
            totalMaturity += p * Math.pow(1 + r, remainingMonths);
        }

        return BigDecimal.valueOf(totalMaturity).setScale(2, RoundingMode.HALF_UP);
    }
}
