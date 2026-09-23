package com.ebank.scheduler;

import com.ebank.service.ScheduledTasksService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final ScheduledTasksService scheduledTasksService;

    public ScheduledTasks(ScheduledTasksService scheduledTasksService) {
        this.scheduledTasksService = scheduledTasksService;
    }

    /**
     * Run daily automated processing at midnight (or configured cron)
     */
    @Scheduled(cron = "${ebank.scheduler.daily-cron:0 0 0 * * ?}")
    public void runDailyAutomatedBankingTasks() {
        log.info("Triggering scheduled automated banking processing...");
        scheduledTasksService.executeDailyAutomations();
    }
}
