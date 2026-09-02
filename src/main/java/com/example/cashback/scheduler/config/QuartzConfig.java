package com.example.cashback.scheduler.config;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail reconciliationJobDetail() {
        return JobBuilder.newJob(com.example.cashback.scheduler.jobs.ReconciliationJob.class)
                .withIdentity("reconciliationJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reconciliationJobTrigger(JobDetail reconciliationJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reconciliationJobDetail)
                .withIdentity("reconciliationTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(2, 0)) // 2 AM daily
                .build();
    }

    @Bean
    public JobDetail offerCleanupJobDetail() {
        return JobBuilder.newJob(com.example.cashback.scheduler.jobs.OfferCleanupJob.class)
                .withIdentity("offerCleanupJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger offerCleanupJobTrigger(JobDetail offerCleanupJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(offerCleanupJobDetail)
                .withIdentity("offerCleanupTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(3, 0)) // 3 AM daily
                .build();
    }

    @Bean
    public JobDetail reportingJobDetail() {
        return JobBuilder.newJob(com.example.cashback.scheduler.jobs.ReportingJob.class)
                .withIdentity("reportingJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reportingJobTrigger(JobDetail reportingJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reportingJobDetail)
                .withIdentity("reportingTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(4, 0)) // 4 AM daily
                .build();
    }

    @Bean
    public JobDetail analyticsJobDetail() {
        return JobBuilder.newJob(com.example.cashback.scheduler.jobs.AnalyticsJob.class)
                .withIdentity("analyticsJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger analyticsJobTrigger(JobDetail analyticsJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(analyticsJobDetail)
                .withIdentity("analyticsTrigger")
                .withSchedule(CronScheduleBuilder.weeklyOnDayAndHourAndMinute(DateBuilder.MONDAY, 5, 0)) // Monday 5 AM
                .build();
    }

    @Bean
    public JobDetail auditJobDetail() {
        return JobBuilder.newJob(com.example.cashback.scheduler.jobs.AuditJob.class)
                .withIdentity("auditJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger auditJobTrigger(JobDetail auditJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(auditJobDetail)
                .withIdentity("auditTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(6, 0)) // 6 AM daily
                .build();
    }

    // Partition creation job (monthly, 28th at midnight)
    @Bean
    public JobDetail auditPartitionJobDetail() {
        return JobBuilder.newJob(com.example.cashback.scheduler.jobs.AuditPartitionJob.class)
                .withIdentity("auditPartitionJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger auditPartitionJobTrigger(JobDetail auditPartitionJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(auditPartitionJobDetail)
                .withIdentity("auditPartitionTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 28 * ?")) // 28th of each month at midnight
                .build();
    }

    // Settlement aggregation job (weekly, Sunday midnight)
    @Bean
    public JobDetail settlementAggregationJobDetail() {
        return JobBuilder.newJob(com.example.cashback.scheduler.jobs.SettlementAggregationJob.class)
                .withIdentity("settlementAggregationJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger settlementAggregationJobTrigger(JobDetail settlementAggregationJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(settlementAggregationJobDetail)
                .withIdentity("settlementAggregationTrigger")
                .withSchedule(CronScheduleBuilder.weeklyOnDayAndHourAndMinute(DateBuilder.SUNDAY, 0, 0)) // Sunday
                                                                                                         // midnight
                .build();
    }

    @Bean
    public JobDetail archivalJobDetail() {
        return JobBuilder.newJob(com.example.cashback.scheduler.jobs.ArchivalJob.class)
                .withIdentity("archivalJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger archivalJobTrigger(JobDetail archivalJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(archivalJobDetail)
                .withIdentity("archivalTrigger")
                .withSchedule(CronScheduleBuilder.monthlyOnDayAndHourAndMinute(1, 1, 0)) // 1st of month, 1 AM
                .build();
    }

}
