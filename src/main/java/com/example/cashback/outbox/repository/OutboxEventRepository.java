package com.example.cashback.outbox.repository;

import com.example.cashback.outbox.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.status IN :statuses AND e.nextAttemptAt <= :now ORDER BY e.createdAt")
    List<OutboxEvent> findReady(@Param("statuses") List<OutboxEvent.Status> statuses,
            @Param("now") LocalDateTime now, Pageable pageable);

}