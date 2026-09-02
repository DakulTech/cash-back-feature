package com.example.cashback.analytics.dto;

import java.util.UUID;
import java.math.BigDecimal;

public class UserAnalyticsDTO {
    private UUID userId;
    private Long transactionCount;
    private BigDecimal totalTransactionAmount;
    private BigDecimal totalCashback;
    private Long rewardCount;
    private BigDecimal totalRewardPoints;
    private Long offersRedeemed;

    public UserAnalyticsDTO(UUID userId,
            Long transactionCount,
            BigDecimal totalTransactionAmount,
            BigDecimal totalCashback,
            Long rewardCount,
            BigDecimal totalRewardPoints,
            Long offersRedeemed) {
        this.userId = userId;
        this.transactionCount = transactionCount;
        this.totalTransactionAmount = totalTransactionAmount;
        this.totalCashback = totalCashback;
        this.rewardCount = rewardCount;
        this.totalRewardPoints = totalRewardPoints;
        this.offersRedeemed = offersRedeemed;
    }

    public UUID getUserId() {
        return userId;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public BigDecimal getTotalTransactionAmount() {
        return totalTransactionAmount;
    }

    public BigDecimal getTotalCashback() {
        return totalCashback;
    }

    public Long getRewardCount() {
        return rewardCount;
    }

    public BigDecimal getTotalRewardPoints() {
        return totalRewardPoints;
    }

    public Long getOffersRedeemed() {
        return offersRedeemed;
    }
}
