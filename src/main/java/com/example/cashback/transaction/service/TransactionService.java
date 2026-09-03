package com.example.cashback.transaction.service;

import com.example.cashback.transaction.model.Transaction;
import com.example.cashback.transaction.dto.TransactionRequest;
import com.example.cashback.transaction.repository.TransactionRepository;
import com.example.cashback.user.model.User;
import com.example.cashback.user.service.UserService;
import com.example.cashback.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final CashbackValidationService cashbackValidationService;
    private final TransactionEligibilityCacheService eligibilityCacheService;
    private final OutboxService outboxService;

    @Transactional
    public Transaction processTransaction(TransactionRequest request) {
        Transaction transaction = request.toTransaction();
        Transaction existing = transactionRepository.findByUserIdAndIdempotencyKey(
                transaction.getUser().getId(), transaction.getIdempotencyKey()).orElse(null);
        if (existing != null) {
            if (!sameSubmission(existing, transaction)) {
                throw new IllegalArgumentException("Idempotency key was already used with a different transaction");
            }
            return existing;
        }

        return processNewTransaction(transaction);
    }

    private Transaction processNewTransaction(Transaction transaction) {
        if (transaction.getId() != null
                && transactionRepository.existsByIdAndCashbackAmountIsNotNull(transaction.getId())) {
            throw new IllegalStateException("Transaction has already received cashback: " + transaction.getId());
        }

        BigDecimal cashback = BigDecimal.ZERO;
        if (transaction.getType() == null || transaction.getAmount() == null) {
            transaction.setCashbackAmount(null);
        } else {
            cashback = cashbackValidationService.calculateCashback(transaction.getType(),
                    transaction.getAmount());
            transaction.setCashbackAmount(cashback);
        }

        if (!cashbackValidationService.validate(transaction)) {
            throw new IllegalArgumentException("Transaction failed cashback integrity validation");
        }

        Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);
        if (!eligibilityCacheService.claim(savedTransaction.getId())) {
            throw new IllegalStateException("Transaction has already received cashback: " + savedTransaction.getId());
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    eligibilityCacheService.release(savedTransaction.getId());
                }
            }
        });

        try {
            User user = savedTransaction.getUser();
            userService.creditReward(user, cashback);
            outboxService.enqueueWarehouseSync(savedTransaction.getId());
        } catch (RuntimeException exception) {
            eligibilityCacheService.release(savedTransaction.getId());
            throw exception;
        }

        return savedTransaction;
    }

    private boolean sameSubmission(Transaction existing, Transaction request) {
        return existing.getUser().getId().equals(request.getUser().getId())
                && existing.getMerchant().getId().equals(request.getMerchant().getId())
                && java.util.Objects.equals(existing.getOffer() == null ? null : existing.getOffer().getId(),
                        request.getOffer() == null ? null : request.getOffer().getId())
                && existing.getAmount().compareTo(request.getAmount()) == 0
                && existing.getType() == request.getType();
    }

    public List<Transaction> getUserTransactions(UUID userId) {
        return transactionRepository.findByUserId(userId);
    }
}
