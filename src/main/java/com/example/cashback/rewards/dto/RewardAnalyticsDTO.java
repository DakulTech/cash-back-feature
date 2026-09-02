package com.example.cashback.rewards.dto;

import java.util.UUID;
import java.math.BigDecimal;

public class RewardAnalyticsDTO {
    private UUID userId;
    private Long rewardCount;
    private BigDecimal totalRewardPoints;

    public RewardAnalyticsDTO(UUID userId, Long rewardCount, BigDecimal totalRewardPoints) {
        this.userId = userId;
        this.rewardCount = rewardCount;
        this.totalRewardPoints = totalRewardPoints;
    }

    public UUID getUserId() {
        return userId;
    }

    public Long getRewardCount() {
        return rewardCount;
    }

    public BigDecimal getTotalRewardPoints() {
        return totalRewardPoints;
    }
}
