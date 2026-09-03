package com.example.cashback.outbox.service;

import com.example.cashback.analytics.service.WarehouseEtlService;
import com.example.cashback.outbox.model.OutboxEvent;
import com.example.cashback.outbox.repository.OutboxEventRepository;
import com.example.cashback.referral.event.ReferralBonusChangedEvent;
import com.example.cashback.referral.service.ReferralBonusCacheService;
import com.example.cashback.user.event.CashbackBalanceChangedEvent;
import com.example.cashback.user.service.CashbackBalanceCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CashbackBalanceCacheService balanceCacheService;

    @Mock
    private ReferralBonusCacheService referralBonusCacheService;

    @Mock
    private WarehouseEtlService warehouseEtlService;

    @InjectMocks
    private OutboxDispatcher dispatcher;

    @Test
    void movesEventToDeadLetterAfterMaxRetries() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventType("UNKNOWN_EVENT")
                .payload("\"tx-1\"")
                .status(OutboxEvent.Status.FAILED)
                .attempts(8)
                .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(repository.findReady(
                eq(List.of(OutboxEvent.Status.PENDING, OutboxEvent.Status.PROCESSING, OutboxEvent.Status.FAILED)),
                any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(event));

        dispatcher.dispatch();

        verify(repository, atLeastOnce()).save(any(OutboxEvent.class));
    }
}
