package com.example.cashback.transaction.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionEligibilityCacheService {

    private static final String KEY_PREFIX = "cashbackEligibility:processed:";

    private final StringRedisTemplate redisTemplate;

    @Value("${cashback.transaction-eligibility-ttl:48h}")
    private Duration markerTtl;

    @PostConstruct
    void validateMarkerTtl() {
        if (markerTtl == null || markerTtl.compareTo(Duration.ofHours(24)) < 0
                || markerTtl.compareTo(Duration.ofHours(48)) > 0) {
            throw new IllegalStateException("cashback.transaction-eligibility-ttl must be between 24h and 48h");
        }
    }

    public boolean claim(UUID transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required for cashback processing");
        }
        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(
                key(transactionId), "processed", markerTtl);
        return Boolean.TRUE.equals(claimed);
    }

    public void release(UUID transactionId) {
        if (transactionId != null) {
            redisTemplate.delete(key(transactionId));
        }
    }

    public void clearAfterReconciliation(UUID transactionId) {
        release(transactionId);
    }

    private String key(UUID transactionId) {
        return KEY_PREFIX + transactionId;
    }
}