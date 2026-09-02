package com.example.cashback.scheduler.jobs;

import com.example.cashback.scheduler.metrics.JobMetrics;
import com.example.cashback.transaction.repository.TransactionRepository;
import com.example.cashback.transaction.service.CashbackValidationService;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class ReconciliationJob implements Job {

    private static final int PAGE_SIZE = 500;

    private final TransactionRepository transactionRepository;
    private final CashbackValidationService cashbackValidationService;
    private final JobMetrics jobMetrics;

    @Override
    public void execute(JobExecutionContext context) {
        long count = transactionRepository.count();
        long anomalies = 0;
        Pageable pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<com.example.cashback.transaction.model.Transaction> transactions;
        do {
            transactions = transactionRepository.findAll(pageRequest);
            anomalies += transactions.getContent().stream()
                    .filter(transaction -> !cashbackValidationService.validate(transaction))
                    .count();
            pageRequest = pageRequest.next();
        } while (transactions.hasNext());

        System.out.println("Reconciliation Job executed. Total transactions: " + count
                + ", anomalies flagged: " + anomalies);

        // Record metric
        jobMetrics.recordJobExecution("ReconciliationJob");
    }
}
