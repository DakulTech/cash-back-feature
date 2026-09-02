package com.example.cashback.transaction.dto;

import java.util.UUID;
import java.math.BigDecimal;

public class TransactionAnalyticsDTO {
    private UUID userId;
    private Long transactionCount;
    private BigDecimal totalAmount;
    private BigDecimal totalCashback;

    public TransactionAnalyticsDTO(UUID userId, Long transactionCount, BigDecimal totalAmount,
            BigDecimal totalCashback) {
        this.userId = userId;
        this.transactionCount = transactionCount;
        this.totalAmount = totalAmount;
        this.totalCashback = totalCashback;
    }

    public UUID getUserId() {
        return userId;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getTotalCashback() {
        return totalCashback;
    }
}
