package com.example.cashback.analytics.service;

import com.example.cashback.analytics.dto.UserAnalyticsDTO;
import com.example.cashback.transaction.service.TransactionAnalyticsService;
import com.example.cashback.rewards.service.RewardAnalyticsService;
import com.example.cashback.merchant.service.MerchantOfferAnalyticsService;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.math.BigDecimal;

@Service
public class UserAnalyticsService {

        private final TransactionAnalyticsService transactionAnalyticsService;
        private final RewardAnalyticsService rewardAnalyticsService;
        private final MerchantOfferAnalyticsService merchantOfferAnalyticsService;

        public UserAnalyticsService(TransactionAnalyticsService transactionAnalyticsService,
                        RewardAnalyticsService rewardAnalyticsService,
                        MerchantOfferAnalyticsService merchantOfferAnalyticsService) {
                this.transactionAnalyticsService = transactionAnalyticsService;
                this.rewardAnalyticsService = rewardAnalyticsService;
                this.merchantOfferAnalyticsService = merchantOfferAnalyticsService;
        }

        public UserAnalyticsDTO getUserAnalytics(UUID userId) {
                // Fetch transaction analytics
                var transactionData = transactionAnalyticsService.getTransactionAnalytics()
                                .stream()
                                .filter(t -> t.getUserId().equals(userId))
                                .findFirst()
                                .orElse(null);

                // Fetch reward analytics
                var rewardData = rewardAnalyticsService.getRewardAnalytics()
                                .stream()
                                .filter(r -> r.getUserId().equals(userId))
                                .findFirst()
                                .orElse(null);

                // Fetch offer analytics (e.g., redemption count)
                Long offersRedeemed = merchantOfferAnalyticsService.getOfferAnalytics()
                                .stream()
                                .filter(o -> o.getOfferId().equals(userId)) // adjust if offers are linked differently
                                .count();

                return new UserAnalyticsDTO(
                                userId,
                                transactionData != null ? transactionData.getTransactionCount() : 0L,
                                transactionData != null ? transactionData.getTotalAmount() : BigDecimal.ZERO,
                                transactionData != null ? transactionData.getTotalCashback() : BigDecimal.ZERO,
                                rewardData != null ? rewardData.getRewardCount() : 0L,
                                rewardData != null ? rewardData.getTotalRewardPoints() : BigDecimal.ZERO,
                                offersRedeemed);
        }
}
