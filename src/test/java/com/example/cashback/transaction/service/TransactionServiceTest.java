package com.example.cashback.transaction.service;

import com.example.cashback.merchant.model.Merchant;
import com.example.cashback.transaction.dto.TransactionRequest;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
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
    void returnsOriginalTransactionForSameUserAndKey() {
        User user = user(UUID.randomUUID());
        Merchant merchant = merchant(UUID.randomUUID());
        Transaction existing = transaction(user, merchant, "request-1", new BigDecimal("100.00"));
        TransactionRequest request = request(user, merchant, "request-1", new BigDecimal("100.00"));
        when(transactionRepository.findByUserIdAndIdempotencyKey(user.getId(), "request-1"))
                .thenReturn(Optional.of(existing));

        TransactionService service = new TransactionService(transactionRepository, userService,
                validationService, eligibilityCacheService, outboxService);

        assertEquals(existing, service.processTransaction(request));
        verify(transactionRepository, never()).saveAndFlush(any(Transaction.class));
        verify(userService, never()).creditReward(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsSameKeyWithDifferentPayload() {
        User user = user(UUID.randomUUID());
        Merchant merchant = merchant(UUID.randomUUID());
        Transaction existing = transaction(user, merchant, "request-1", new BigDecimal("100.00"));
        TransactionRequest request = request(user, merchant, "request-1", new BigDecimal("101.00"));
        when(transactionRepository.findByUserIdAndIdempotencyKey(user.getId(), "request-1"))
                .thenReturn(Optional.of(existing));

        TransactionService service = new TransactionService(transactionRepository, userService,
                validationService, eligibilityCacheService, outboxService);

        assertThrows(IllegalArgumentException.class, () -> service.processTransaction(request));
        verify(transactionRepository, never()).saveAndFlush(any(Transaction.class));
        verify(userService, never()).creditReward(any(), any());
    }

    @Test
    void allowsSameKeyForDifferentUsers() {
        User firstUser = user(UUID.randomUUID());
        User secondUser = user(UUID.randomUUID());
        Merchant merchant = merchant(UUID.randomUUID());
        TransactionRequest firstRequest = request(firstUser, merchant, "request-1", new BigDecimal("100.00"));
        TransactionRequest secondRequest = request(secondUser, merchant, "request-1", new BigDecimal("100.00"));
        Transaction firstTransaction = transaction(firstUser, merchant, "request-1", new BigDecimal("100.00"));
        Transaction secondTransaction = transaction(secondUser, merchant, "request-1", new BigDecimal("100.00"));
        secondTransaction.setId(UUID.randomUUID());
        when(transactionRepository.findByUserIdAndIdempotencyKey(firstUser.getId(), "request-1"))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByUserIdAndIdempotencyKey(secondUser.getId(), "request-1"))
                .thenReturn(Optional.empty());
        when(validationService.calculateCashback(any(), any())).thenReturn(new BigDecimal("2.50"));
        when(validationService.validate(any())).thenReturn(true);
        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenReturn(firstTransaction, secondTransaction);
        when(eligibilityCacheService.claim(any(UUID.class))).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();

        TransactionService service = new TransactionService(transactionRepository, userService,
                validationService, eligibilityCacheService, outboxService);

        service.processTransaction(firstRequest);
        service.processTransaction(secondRequest);

        verify(userService).creditReward(firstUser, new BigDecimal("2.50"));
        verify(userService).creditReward(secondUser, new BigDecimal("2.50"));
    }

    @Test
    void concurrentSubmissionsCreditOnlyOnceWhenRedisClaimWinsOnce() throws Exception {
        User user = user(UUID.randomUUID());
        Merchant merchant = merchant(UUID.randomUUID());
        TransactionRequest request = request(user, merchant, "request-1", new BigDecimal("100.00"));
        AtomicBoolean claim = new AtomicBoolean(true);
        AtomicInteger saves = new AtomicInteger();
        when(transactionRepository.findByUserIdAndIdempotencyKey(user.getId(), "request-1"))
                .thenReturn(Optional.empty());
        when(validationService.calculateCashback(TransactionType.AIRTIME, new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("2.50"));
        when(validationService.validate(any())).thenReturn(true);
        when(transactionRepository.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saves.incrementAndGet();
            return saved;
        });
        when(eligibilityCacheService.claim(any(UUID.class))).thenAnswer(invocation -> claim.getAndSet(false));

        TransactionService service = new TransactionService(transactionRepository, userService,
                validationService, eligibilityCacheService, outboxService);

        IntStream.range(0, 2).parallel().forEach(index -> {
            try {
                service.processTransaction(request);
            } catch (IllegalStateException expectedForLosingClaim) {
            }
        });

        assertEquals(2, saves.get());
        verify(userService).creditReward(same(user), eq(new BigDecimal("2.50")));
        verify(userService, org.mockito.Mockito.times(1))
                .creditReward(any(User.class), any(BigDecimal.class));
    }

    // test helpers
    private static User user(UUID id) {
        return User.builder().id(id).rewardBalance(BigDecimal.ZERO).build();
    }

    private static Merchant merchant(UUID id) {
        return Merchant.builder().id(id).name("merchant-" + id).email(id + "@example.com").build();
    }

    private static Transaction transaction(User user, Merchant merchant, String key, BigDecimal amount) {
        return Transaction.builder().id(UUID.randomUUID()).user(user).merchant(merchant)
                .amount(amount).type(TransactionType.AIRTIME).idempotencyKey(key).build();
    }

    private static TransactionRequest request(User user, Merchant merchant, String key, BigDecimal amount) {
        return new TransactionRequest(user, merchant, null, amount, TransactionType.AIRTIME, key);
    }

}
