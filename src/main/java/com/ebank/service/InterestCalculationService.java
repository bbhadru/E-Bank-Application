package com.ebank.service;

import com.ebank.model.SavingsAccount;
import com.ebank.repository.SavingsAccountRepository;
import com.ebank.util.InterestCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InterestCalculationService {

    private static final Logger log = LoggerFactory.getLogger(InterestCalculationService.class);

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsAccountService savingsAccountService;
    private final AuditService auditService;

    public InterestCalculationService(SavingsAccountRepository savingsAccountRepository,
                                      SavingsAccountService savingsAccountService,
                                      AuditService auditService) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsAccountService = savingsAccountService;
        this.auditService = auditService;
    }

    @Transactional
    public void calculateAndPostDailySavingsInterest() {
        List<SavingsAccount> accounts = savingsAccountRepository.findByStatus("ACTIVE");
        log.info("Starting daily interest calculation for {} active savings accounts", accounts.size());

        int count = 0;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (SavingsAccount account : accounts) {
            BigDecimal dailyInterest = InterestCalculator.calculateDailySbInterest(
                    account.getBalance(), account.getInterestRate());

            if (dailyInterest.compareTo(new BigDecimal("0.01")) >= 0) {
                savingsAccountService.creditInterest(account, dailyInterest);
                totalInterest = totalInterest.add(dailyInterest);
                count++;
            }
        }

        log.info("Completed daily interest calculation. Credited interest to {} accounts, total amount: {}", count, totalInterest);
        auditService.log(null, "DAILY_INTEREST_POSTED", "SavingsAccount", null, null,
                "Accounts: " + count + ", Total: " + totalInterest);
    }
}
