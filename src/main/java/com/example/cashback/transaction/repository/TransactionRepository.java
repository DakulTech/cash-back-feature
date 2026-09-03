package com.example.cashback.transaction.repository;

import com.example.cashback.transaction.model.Transaction;
import com.example.cashback.transaction.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Find all transactions for a given user
    List<Transaction> findByUserId(UUID userId);

    // Find all transactions by type
    List<Transaction> findByType(TransactionType type);

    // Find transactions created before a cutoff date
    List<Transaction> findByCreatedAtBefore(LocalDateTime cutoff);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt > :createdAt "
            + "OR (t.createdAt = :createdAt AND t.id > :id) "
            + "ORDER BY t.createdAt ASC, t.id ASC")
    Page<Transaction> findAfterWatermark(@Param("createdAt") LocalDateTime createdAt,
            @Param("id") UUID id, Pageable pageable);

    boolean existsByIdAndCashbackAmountIsNotNull(UUID id);

    java.util.Optional<Transaction> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    // Find transactions by user and type
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.type = :type")
    List<Transaction> findByUserAndType(@Param("userId") UUID userId, @Param("type") TransactionType type);

}
