package com.ebank.service;

import com.ebank.model.FixedDeposit;
import com.ebank.repository.FixedDepositRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduledTasksService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasksService.class);

    private final InterestCalculationService interestCalculationService;
    private final FixedDepositRepository fixedDepositRepository;
    private final FixedDepositService fixedDepositService;
    private final LoanService loanService;
    private final AuditService auditService;

    public ScheduledTasksService(InterestCalculationService interestCalculationService,
                                 FixedDepositRepository fixedDepositRepository,
                                 FixedDepositService fixedDepositService,
                                 LoanService loanService,
                                 AuditService auditService) {
        this.interestCalculationService = interestCalculationService;
        this.fixedDepositRepository = fixedDepositRepository;
        this.fixedDepositService = fixedDepositService;
        this.loanService = loanService;
        this.auditService = auditService;
    }

    @Transactional
    public void executeDailyAutomations() {
        LocalDate today = LocalDate.now();
        log.info("Running daily automated banking background jobs for date: {}", today);

        // 1. Calculate & credit daily savings interest
        try {
            interestCalculationService.calculateAndPostDailySavingsInterest();
        } catch (Exception ex) {
            log.error("Error in daily savings interest calculation", ex);
        }

        // 2. Check and mature FDs
        try {
            List<FixedDeposit> maturingList = fixedDepositRepository.findMaturingDeposits(today);
            log.info("Found {} maturing fixed deposits", maturingList.size());
            for (FixedDeposit fd : maturingList) {
                fixedDepositService.matureOrCloseDeposit(fd.getFdId());
            }
        } catch (Exception ex) {
            log.error("Error processing maturing fixed deposits", ex);
        }

        // 3. Check and flag NPA loans (> 90 days overdue)
        try {
            loanService.checkAndFlagNpa(today);
        } catch (Exception ex) {
            log.error("Error in checking loan NPAs", ex);
        }

        auditService.log(null, "DAILY_AUTOMATION_COMPLETED", "System", null, null, "Date: " + today);
        log.info("Daily automated banking background jobs completed successfully.");
    }
}
