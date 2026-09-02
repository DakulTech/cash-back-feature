package com.example.cashback.merchant.dto;

import java.util.UUID;
import java.math.BigDecimal;

public class OfferAnalyticsDTO {
    private UUID offerId;
    private Long redemptionCount;
    private BigDecimal totalCashback;
    private BigDecimal averageTransactionAmount;

    public OfferAnalyticsDTO(UUID offerId, Long redemptionCount, BigDecimal totalCashback,
            BigDecimal averageTransactionAmount) {
        this.offerId = offerId;
        this.redemptionCount = redemptionCount;
        this.totalCashback = totalCashback;
        this.averageTransactionAmount = averageTransactionAmount;
    }

    public UUID getOfferId() {
        return offerId;
    }

    public Long getRedemptionCount() {
        return redemptionCount;
    }

    public BigDecimal getTotalCashback() {
        return totalCashback;
    }

    public BigDecimal getAverageTransactionAmount() {
        return averageTransactionAmount;
    }
}
