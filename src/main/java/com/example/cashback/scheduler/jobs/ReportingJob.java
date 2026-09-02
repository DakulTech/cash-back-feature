package com.example.cashback.scheduler.jobs;

import com.example.cashback.analytics.service.WarehouseEtlService;
import com.example.cashback.scheduler.metrics.JobMetrics;

import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class ReportingJob implements Job {

    private final WarehouseEtlService warehouseEtlService;
    private final JobMetrics jobMetrics;

    @Override
    public void execute(JobExecutionContext context) {
        int loadedTransactions = warehouseEtlService.loadTransactions();
        warehouseEtlService.exportDailyReport();
        System.out.println("Reporting Job: loaded " + loadedTransactions
                + " transactions and exported the daily warehouse report.");

        // Record metric
        jobMetrics.recordJobExecution("ReportingJob");
    }
}
