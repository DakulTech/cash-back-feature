package com.example.cashback.outbox.service;

import com.example.cashback.outbox.model.OutboxEvent;
import com.example.cashback.outbox.repository.OutboxEventRepository;
import com.example.cashback.analytics.service.WarehouseEtlService;
import com.example.cashback.referral.event.ReferralBonusChangedEvent;
import com.example.cashback.referral.service.ReferralBonusCacheService;
import com.example.cashback.user.event.CashbackBalanceChangedEvent;
import com.example.cashback.user.service.CashbackBalanceCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxDispatcher {

    private static final int BATCH_SIZE = 100;
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final CashbackBalanceCacheService balanceCacheService;
    private final ReferralBonusCacheService referralBonusCacheService;
    private final WarehouseEtlService warehouseEtlService;

    @Scheduled(fixedDelayString = "${cashback.outbox-dispatch-delay:5000}")
    @Transactional
    public void dispatch() {
        List<OutboxEvent> events = repository.findReady(
                List.of(OutboxEvent.Status.PENDING, OutboxEvent.Status.PROCESSING),
                LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent event : events) {
            try {
                event.setStatus(OutboxEvent.Status.PROCESSING);
                repository.save(event);
                dispatch(event);
                event.setStatus(OutboxEvent.Status.COMPLETED);
                event.setNextAttemptAt(LocalDateTime.now().plusYears(100));
                repository.save(event);
            } catch (RuntimeException exception) {
                int attempts = event.getAttempts() + 1;
                event.setAttempts(attempts);
                event.setStatus(OutboxEvent.Status.PENDING);
                event.setNextAttemptAt(LocalDateTime.now().plusSeconds(Math.min(300, 1L << Math.min(attempts, 8))));
                repository.save(event);
            }
        }
    }

    private void dispatch(OutboxEvent event) {
        if ("CASHBACK_BALANCE_CHANGED".equals(event.getEventType())) {
            balanceCacheService.refresh(objectMapper.convertValue(read(event), CashbackBalanceChangedEvent.class));
        } else if ("REFERRAL_BONUS_CHANGED".equals(event.getEventType())) {
            referralBonusCacheService.refresh(objectMapper.convertValue(read(event), ReferralBonusChangedEvent.class));
        } else if ("WAREHOUSE_SYNC_REQUESTED".equals(event.getEventType())) {
            warehouseEtlService.loadTransactions();
        } else {
            throw new IllegalArgumentException("Unknown outbox event type: " + event.getEventType());
        }
    }

    private Object read(OutboxEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to deserialize outbox event", exception);
        }
    }
}