package com.example.cashback.user.service;

import com.example.cashback.user.event.CashbackBalanceChangedEvent;
import com.example.cashback.user.model.User;
import com.example.cashback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashbackBalanceCacheService {

    public static final String CACHE_NAME = "cashbackBalances";

    private final CacheManager cacheManager;
    private final UserRepository userRepository;

    public void refresh(CashbackBalanceChangedEvent event) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            User user = userRepository.findById(event.userId()).orElse(null);
            if (user != null) {
                cache.put(event.userId(), user.getRewardBalance());
            }
        }
    }
}