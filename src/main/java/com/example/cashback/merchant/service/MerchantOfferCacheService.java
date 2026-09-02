package com.example.cashback.merchant.service;

import com.example.cashback.merchant.model.MerchantOffer;
import com.example.cashback.merchant.repository.MerchantOfferRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantOfferCacheService {

    private static final String KEY_PREFIX = "cashbackOffers:merchant:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final MerchantOfferRepository offerRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<MerchantOffer> getActiveOffers(UUID merchantId) {
        String key = KEY_PREFIX + merchantId;
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                List<MerchantOffer> offers = deserialize(cached);
                if (offers != null) {
                    return offers;
                }
                redisTemplate.delete(key);
            }
        } catch (RuntimeException ignored) {
            // Redis is an optimization; use PostgreSQL when the cache is unavailable.
        }

        List<MerchantOffer> offers = offerRepository.findByMerchantId(merchantId).stream()
                .filter(this::isActive)
                .toList();
        cache(key, offers);
        return offers;
    }

    public void evict(UUID merchantId) {
        redisTemplate.delete(KEY_PREFIX + merchantId);
    }

    private boolean isActive(MerchantOffer offer) {
        return Boolean.TRUE.equals(offer.getActive())
                && (offer.getExpiryDate() == null || !offer.getExpiryDate().isBefore(LocalDate.now()));
    }

    private void cache(String key, List<MerchantOffer> offers) {
        try {
            List<CachedOffer> values = offers.stream()
                    .map(offer -> new CachedOffer(offer.getId(), offer.getMerchant().getId(),
                            offer.getCashbackRate(), offer.getExpiryDate(), offer.getActive()))
                    .toList();
            Duration ttl = offers.stream()
                    .map(MerchantOffer::getExpiryDate)
                    .filter(date -> date != null)
                    .map(date -> Duration.between(java.time.LocalDateTime.now(), date.plusDays(1).atStartOfDay()))
                    .filter(duration -> !duration.isNegative() && !duration.isZero())
                    .min(Duration::compareTo)
                    .orElse(DEFAULT_TTL);
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(values), ttl);
        } catch (Exception ignored) {
            // Redis is an optimization; a cache serialization failure must not block offer
            // lookup.
        }
    }

    private List<MerchantOffer> deserialize(String cached) {
        try {
            return objectMapper.readValue(cached, new TypeReference<List<CachedOffer>>() {
            }).stream().filter(this::isActive).map(this::toOffer).toList();
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean isActive(CachedOffer offer) {
        return Boolean.TRUE.equals(offer.active())
                && (offer.expiryDate() == null || !offer.expiryDate().isBefore(LocalDate.now()));
    }

    private MerchantOffer toOffer(CachedOffer offer) {
        return MerchantOffer.builder()
                .id(offer.id())
                .merchant(com.example.cashback.merchant.model.Merchant.builder().id(offer.merchantId()).build())
                .cashbackRate(offer.cashbackRate())
                .expiryDate(offer.expiryDate())
                .active(offer.active())
                .build();
    }

    private record CachedOffer(UUID id, UUID merchantId, BigDecimal cashbackRate,
            LocalDate expiryDate, Boolean active) {
    }
}