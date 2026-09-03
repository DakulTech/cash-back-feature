package com.example.cashback.scheduler.jobs;

import com.example.cashback.outbox.service.ReconciliationService;
import com.example.cashback.outbox.service.ReconciliationSummary;
import com.example.cashback.scheduler.metrics.JobMetrics;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class ReconciliationJob implements Job {

    private final ReconciliationService reconciliationService;
    private final JobMetrics jobMetrics;

    @Override
    public void execute(JobExecutionContext context) {
        ReconciliationSummary summary = reconciliationService.reconcile();

        System.out.println("Reconciliation Job executed. Total transactions: " + summary.transactionCount()
                + ", anomalies flagged: " + summary.anomaliesDetected()
                + ", dead-letter events: " + summary.deadLetterCount());

        jobMetrics.recordJobExecution("ReconciliationJob");
    }
}
