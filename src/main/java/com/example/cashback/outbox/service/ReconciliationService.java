package com.example.cashback.outbox.service;

import com.example.cashback.outbox.model.OutboxEvent;
import com.example.cashback.outbox.repository.OutboxEventRepository;
import com.example.cashback.transaction.model.Transaction;
import com.example.cashback.transaction.repository.TransactionRepository;
import com.example.cashback.transaction.service.CashbackValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private static final int PAGE_SIZE = 500;

    private final TransactionRepository transactionRepository;
    private final CashbackValidationService cashbackValidationService;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional(readOnly = true)
    public ReconciliationSummary reconcile() {
        long count = transactionRepository.count();
        long anomalies = 0;
        Pageable pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<Transaction> transactions;
        do {
            transactions = transactionRepository.findAll(pageRequest);
            anomalies += transactions.getContent().stream()
                    .filter(transaction -> !cashbackValidationService.validate(transaction))
                    .count();
            pageRequest = pageRequest.next();
        } while (transactions.hasNext());

        long deadLetterCount = outboxEventRepository.countByStatus(OutboxEvent.Status.DEAD_LETTER);
        return new ReconciliationSummary(count, anomalies, deadLetterCount);
    }
}
