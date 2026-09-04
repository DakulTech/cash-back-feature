package com.example.cashback.outbox.service;

import com.example.cashback.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({ OutboxService.class, JacksonAutoConfiguration.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxTransactionIntegrationTest {

    @Autowired
    private OutboxEventRepository repository;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clearOutbox() {
        repository.deleteAll();
    }

    @Test
    void commitsOutboxRowWithBusinessTransaction() {
        transactionTemplate.executeWithoutResult(status -> outboxService.enqueueWarehouseSync(UUID.randomUUID()));

        assertEquals(1, repository.count());
    }

    @Test
    void rollbackLeavesNoOutboxRow() {
        transactionTemplate.executeWithoutResult(status -> {
            outboxService.enqueueWarehouseSync(UUID.randomUUID());
            assertEquals(1, repository.count());
            status.setRollbackOnly();
        });

        assertEquals(0, repository.count());
    }

    @Test
    void doesNotDependOnPostCommitCallbackForDurability() {
        transactionTemplate.executeWithoutResult(status -> {
            int synchronizationsBeforeEnqueue = TransactionSynchronizationManager.getSynchronizations().size();
            outboxService.enqueueWarehouseSync(UUID.randomUUID());
            assertEquals(1, repository.count());
            assertEquals(synchronizationsBeforeEnqueue,
                    TransactionSynchronizationManager.getSynchronizations().size());
        });

        assertEquals(1, repository.count());
    }
}