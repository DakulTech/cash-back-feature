package com.example.cashback.rewards.service;

import com.example.cashback.rewards.dto.RewardAnalyticsDTO;
import com.example.cashback.analytics.repository.WarehouseAnalyticsRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RewardAnalyticsService {

    private final WarehouseAnalyticsRepository warehouseAnalyticsRepository;

    public RewardAnalyticsService(WarehouseAnalyticsRepository warehouseAnalyticsRepository) {
        this.warehouseAnalyticsRepository = warehouseAnalyticsRepository;
    }

    public List<RewardAnalyticsDTO> getRewardAnalytics() {
        List<Object[]> totals = warehouseAnalyticsRepository.totalRewardsPerUser();

        List<RewardAnalyticsDTO> analytics = new ArrayList<>();

        for (Object[] row : totals) {
            UUID userId = (UUID) row[0];
            Long rewardCount = (Long) row[1];
            java.math.BigDecimal totalPoints = (java.math.BigDecimal) row[2];

            analytics.add(new RewardAnalyticsDTO(userId, rewardCount, totalPoints));
        }

        return analytics;
    }
}
