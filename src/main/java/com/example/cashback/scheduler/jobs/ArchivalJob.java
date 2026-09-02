package com.example.cashback.scheduler.jobs;

import com.example.cashback.scheduler.metrics.JobMetrics;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.DisallowConcurrentExecution;
import com.example.cashback.analytics.service.WarehouseArchiveService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class ArchivalJob implements Job {

        private final WarehouseArchiveService warehouseArchiveService;
        private final JobMetrics jobMetrics;

        @Override
        public void execute(JobExecutionContext context) {
                LocalDate cutoffDate = LocalDate.now().minusMonths(6); // archive data older than 6 months

                int archivedRows = warehouseArchiveService.archiveBefore(cutoffDate);

                System.out.println("ArchivalJob: Moved " + archivedRows
                                + " rows to the warehouse archive; cutoff " + cutoffDate);

                // Record Prometheus metric
                jobMetrics.recordJobExecution("ArchivalJob");
        }
}
