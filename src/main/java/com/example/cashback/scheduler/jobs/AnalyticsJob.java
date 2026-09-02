package com.example.cashback.scheduler.jobs;

import com.example.cashback.scheduler.metrics.JobMetrics;
import com.example.cashback.analytics.service.WarehouseEtlService;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class AnalyticsJob implements Job {

    private final WarehouseEtlService warehouseEtlService;
    private final JobMetrics jobMetrics;

    @Override
    public void execute(JobExecutionContext context) {
        int loadedTransactions = warehouseEtlService.loadTransactions();
        System.out.println("Analytics Job: loaded " + loadedTransactions + " transactions into warehouse");

        // Record metric
        jobMetrics.recordJobExecution("AnalyticsJob");
    }
}
