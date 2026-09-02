package com.example.cashback.outbox.service;

import com.example.cashback.outbox.model.OutboxEvent;
import com.example.cashback.outbox.repository.OutboxEventRepository;
import com.example.cashback.referral.event.ReferralBonusChangedEvent;
import com.example.cashback.user.event.CashbackBalanceChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueue(CashbackBalanceChangedEvent event) {
        save("CASHBACK_BALANCE_CHANGED", event);
    }

    @Transactional
    public void enqueue(ReferralBonusChangedEvent event) {
        save("REFERRAL_BONUS_CHANGED", event);
    }

    @Transactional
    public void enqueueWarehouseSync(UUID transactionId) {
        save("WAREHOUSE_SYNC_REQUESTED", transactionId);
    }

    private void save(String eventType, Object event) {
        try {
            repository.save(OutboxEvent.builder()
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(event))
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize outbox event", exception);
        }
    }
}