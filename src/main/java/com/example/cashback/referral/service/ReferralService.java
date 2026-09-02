package com.example.cashback.referral.service;

import com.example.cashback.referral.model.Referral;
import com.example.cashback.referral.event.ReferralBonusChangedEvent;
import com.example.cashback.referral.repository.ReferralRepository;
import com.example.cashback.outbox.service.OutboxService;
import com.example.cashback.rewards.service.RewardService;
import com.example.cashback.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final RewardService rewardService;
    private final UserService userService;
    private final OutboxService outboxService;

    @Value("${cashback.referral-local-max-bonus:8000}")
    private BigDecimal localMaximumBonus;

    @Value("${cashback.referral-uk-max-bonus:10000}")
    private BigDecimal ukMaximumBonus;

    @Transactional
    public Referral registerReferral(String referralCode, UUID referredUserId, boolean isUKReferral) {
        if (referralCode == null || referralCode.isBlank()) {
            throw new IllegalArgumentException("Referral code is required");
        }
        if (referralRepository.existsByReferredUserId(referredUserId)) {
            throw new IllegalArgumentException("User has already been referred");
        }

        UUID referrerId = userService.findByReferralCode(referralCode).getId();
        BigDecimal bonus = isUKReferral ? new BigDecimal("10000.00") : new BigDecimal("8000.00");
        BigDecimal maximumBonus = isUKReferral ? ukMaximumBonus : localMaximumBonus;
        referralRepository.initializeBonusTotal(referrerId);
        if (referralRepository.reserveBonus(referrerId, bonus, maximumBonus) != 1) {
            throw new IllegalStateException("Referral bonus limit reached for code " + referralCode);
        }
        BigDecimal totalBonus = referralRepository.reservedBonusTotal(referrerId);

        rewardService.creditReward(referrerId, bonus, "Referral Bonus");

        Referral referral = Referral.builder()
                .referrerId(referrerId)
                .referredUserId(referredUserId)
                .bonusAmount(bonus)
                .build();

        Referral savedReferral = referralRepository.save(referral);
        outboxService.enqueue(new ReferralBonusChangedEvent(referralCode, totalBonus));
        return savedReferral;
    }

    public List<Referral> getReferrals(UUID referrerId) {
        return referralRepository.findByReferrerId(referrerId);
    }
}
