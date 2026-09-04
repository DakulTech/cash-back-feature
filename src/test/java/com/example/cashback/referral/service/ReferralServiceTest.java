package com.example.cashback.referral.service;

import com.example.cashback.referral.repository.ReferralRepository;
import com.example.cashback.rewards.service.RewardService;
import com.example.cashback.user.model.User;
import com.example.cashback.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import com.example.cashback.outbox.service.OutboxService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private RewardService rewardService;

    @Mock
    private UserService userService;

    @Mock
    private OutboxService outboxService;

    private ReferralService referralService;

    @BeforeEach
    void setUp() {
        referralService = new ReferralService(referralRepository, rewardService, userService, outboxService);
        ReflectionTestUtils.setField(referralService, "localMaximumBonus", new BigDecimal("8000.00"));
        ReflectionTestUtils.setField(referralService, "ukMaximumBonus", new BigDecimal("10000.00"));
    }

    @Test
    void rejectsLocalReferralWhenPostgresUsageHasReachedLimit() {
        UUID referrerId = UUID.randomUUID();
        UUID referredUserId = UUID.randomUUID();
        User referrer = User.builder().id(referrerId).build();
        when(referralRepository.existsByReferredUserId(referredUserId)).thenReturn(false);
        when(userService.findByReferralCode("LOCAL123")).thenReturn(referrer);
        when(referralRepository.reserveBonus(referrerId, new BigDecimal("8000.00"),
                new BigDecimal("8000.00"))).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> referralService.registerReferral("LOCAL123", referredUserId, false));

        verify(rewardService, never()).creditReward(referrerId, new BigDecimal("8000.00"), "Referral Bonus");
        verify(referralRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(outboxService, never()).enqueue(org.mockito.ArgumentMatchers.any(
                com.example.cashback.referral.event.ReferralBonusChangedEvent.class));
    }
}
