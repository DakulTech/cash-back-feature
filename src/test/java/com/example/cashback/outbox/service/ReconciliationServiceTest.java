package com.example.cashback.outbox.service;

import com.example.cashback.outbox.model.OutboxEvent;
import com.example.cashback.outbox.repository.OutboxEventRepository;
import com.example.cashback.transaction.model.Transaction;
import com.example.cashback.transaction.repository.TransactionRepository;
import com.example.cashback.transaction.service.CashbackValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CashbackValidationService cashbackValidationService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    void recordsTransactionDriftAndDeadLetterCount() {
        Transaction goodTransaction = Transaction.builder().id(java.util.UUID.randomUUID()).build();
        Transaction badTransaction = Transaction.builder().id(java.util.UUID.randomUUID()).build();

        when(transactionRepository.count()).thenReturn(2L);
        when(transactionRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(goodTransaction, badTransaction), PageRequest.of(0, 500), 2));
        when(cashbackValidationService.validate(goodTransaction)).thenReturn(true);
        when(cashbackValidationService.validate(badTransaction)).thenReturn(false);
        when(outboxEventRepository.countByStatus(OutboxEvent.Status.DEAD_LETTER)).thenReturn(3L);

        ReconciliationSummary summary = reconciliationService.reconcile();

        assertEquals(2L, summary.transactionCount());
        assertEquals(1L, summary.anomaliesDetected());
        assertEquals(3L, summary.deadLetterCount());
    }
}
