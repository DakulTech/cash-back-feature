package com.example.cashback.transaction.service;

import com.example.cashback.transaction.model.Transaction;
import com.example.cashback.transaction.model.TransactionType;
import com.example.cashback.transaction.repository.TransactionRepository;
import com.example.cashback.user.model.User;
import com.example.cashback.user.service.UserService;
import com.example.cashback.outbox.service.OutboxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @Mock
    private CashbackValidationService validationService;

    @Mock
    private TransactionEligibilityCacheService eligibilityCacheService;

    @Mock
    private OutboxService outboxService;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rejectsTransactionThatAlreadyHasCashback() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder().id(transactionId).build();
        when(transactionRepository.existsByIdAndCashbackAmountIsNotNull(transactionId)).thenReturn(true);

        TransactionService service = new TransactionService(transactionRepository, userService,
                validationService, eligibilityCacheService, outboxService);

        assertThrows(IllegalStateException.class, () -> service.processTransaction(transaction));
        verify(transactionRepository, never()).saveAndFlush(transaction);
        verify(userService, never()).creditReward(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void claimsTransactionBeforeCreditingUser() {
        UUID transactionId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).rewardBalance(BigDecimal.ZERO).build();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .user(user)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.AIRTIME)
                .build();
        when(transactionRepository.existsByIdAndCashbackAmountIsNotNull(transactionId)).thenReturn(false);
        when(validationService.calculateCashback(TransactionType.AIRTIME, new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("2.50"));
        when(validationService.validate(transaction)).thenReturn(true);
        when(transactionRepository.saveAndFlush(transaction)).thenReturn(transaction);
        when(eligibilityCacheService.claim(transactionId)).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();

        TransactionService service = new TransactionService(transactionRepository, userService,
                validationService, eligibilityCacheService, outboxService);

        service.processTransaction(transaction);

        verify(eligibilityCacheService).claim(transactionId);
        verify(userService).creditReward(user, new BigDecimal("2.50"));
    }
}
