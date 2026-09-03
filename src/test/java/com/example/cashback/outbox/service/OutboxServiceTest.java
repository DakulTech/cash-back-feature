package com.example.cashback.outbox.service;

import com.example.cashback.outbox.model.OutboxEvent;
import com.example.cashback.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository repository;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void savesWarehouseSyncImmediatelyWithinActiveTransaction() {
        OutboxService service = new OutboxService(repository, new ObjectMapper());
        UUID transactionId = UUID.randomUUID();

        TransactionSynchronizationManager.initSynchronization();
        service.enqueueWarehouseSync(transactionId);

        verify(repository).save(any(OutboxEvent.class));
        org.junit.jupiter.api.Assertions.assertTrue(
                TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test
    void surfacesPersistenceFailureInsideCurrentTransaction() {
        OutboxService service = new OutboxService(repository, new ObjectMapper());
        doThrow(new IllegalStateException("database unavailable"))
                .when(repository).save(any(OutboxEvent.class));

        TransactionSynchronizationManager.initSynchronization();

        assertThrows(IllegalStateException.class,
                () -> service.enqueueWarehouseSync(UUID.randomUUID()));
        verify(repository).save(any(OutboxEvent.class));
        verify(repository, never()).saveAll(any());
        org.junit.jupiter.api.Assertions.assertTrue(
                TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }
}
