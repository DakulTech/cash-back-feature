package com.example.cashback.merchant.service;

import com.example.cashback.merchant.dto.OfferAnalyticsDTO;
import com.example.cashback.analytics.repository.WarehouseAnalyticsRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MerchantOfferAnalyticsService {

    private final WarehouseAnalyticsRepository warehouseAnalyticsRepository;

    public MerchantOfferAnalyticsService(WarehouseAnalyticsRepository warehouseAnalyticsRepository) {
        this.warehouseAnalyticsRepository = warehouseAnalyticsRepository;
    }

    public List<OfferAnalyticsDTO> getOfferAnalytics() {
        List<OfferAnalyticsDTO> analytics = new ArrayList<>();

        for (Object[] row : warehouseAnalyticsRepository.offerAnalytics()) {
            UUID offerId = (UUID) row[0];
            Long redemptionCount = (Long) row[1];
            java.math.BigDecimal totalCashback = (java.math.BigDecimal) row[2];
            java.math.BigDecimal avgAmount = (java.math.BigDecimal) row[3];

            analytics.add(new OfferAnalyticsDTO(offerId, redemptionCount, totalCashback, avgAmount));
        }

        return analytics;
    }
}
