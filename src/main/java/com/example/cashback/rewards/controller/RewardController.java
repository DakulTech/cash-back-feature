package com.example.cashback.rewards.controller;

import com.example.cashback.rewards.dto.RewardAnalyticsDTO;
import com.example.cashback.rewards.model.Reward;
import com.example.cashback.rewards.service.RewardService;
import com.example.cashback.rewards.service.RewardAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;
    private final RewardAnalyticsService rewardAnalyticsService;

    @PostMapping("/credit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reward> creditReward(
            @RequestParam UUID userId,
            @RequestParam BigDecimal amount,
            @RequestParam String description) {
        return ResponseEntity.ok(rewardService.creditReward(userId, amount, description));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@accessControl.canAccessUser(#userId)")
    public ResponseEntity<List<Reward>> getUserRewards(@PathVariable UUID userId) {
        return ResponseEntity.ok(rewardService.getUserRewards(userId));
    }

    @GetMapping("/user/{userId}/balance")
    @PreAuthorize("@accessControl.canAccessUser(#userId)")
    public ResponseEntity<BigDecimal> getUserBalance(@PathVariable UUID userId) {
        return ResponseEntity.ok(rewardService.getUserBalance(userId));
    }

    // Unified analytics endpoint
    @GetMapping("/analytics")
    public ResponseEntity<List<RewardAnalyticsDTO>> getRewardAnalytics() {
        return ResponseEntity.ok(rewardAnalyticsService.getRewardAnalytics());
    }
}
