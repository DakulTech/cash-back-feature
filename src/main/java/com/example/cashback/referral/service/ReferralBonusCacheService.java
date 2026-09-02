package com.example.cashback.referral.service;

import com.example.cashback.referral.event.ReferralBonusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ReferralBonusCacheService {

    private static final String KEY_PREFIX = "referralBonus:code:";
    private final StringRedisTemplate redisTemplate;

    @Value("${cashback.referral-cache-ttl:30d}")
    private Duration cacheTtl;

    public void refresh(ReferralBonusChangedEvent event) {
        redisTemplate.opsForValue().set(key(event.referralCode()),
                String.valueOf(event.totalBonus()), cacheTtl);
    }

    private String key(String referralCode) {
        return KEY_PREFIX + referralCode.trim().toLowerCase(java.util.Locale.ROOT);
    }
}