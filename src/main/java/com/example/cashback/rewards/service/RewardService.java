package com.example.cashback.rewards.service;

import com.example.cashback.rewards.model.Reward;
import com.example.cashback.rewards.repository.RewardRepository;
import com.example.cashback.user.model.User;
import com.example.cashback.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;
    private final UserService userService;

    @Transactional
    public Reward creditReward(UUID userId, BigDecimal amount, String description) {
        User user = userService.findById(userId);
        userService.creditReward(user, amount);

        Reward reward = Reward.builder()
                .userId(userId)
                .amount(amount)
                .description(description)
                .build();

        Reward result = rewardRepository.save(reward);
        return result;
    }

    public List<Reward> getUserRewards(UUID userId) {
        return rewardRepository.findByUserId(userId);
    }

    public BigDecimal getUserBalance(UUID userId) {
        return userService.getRewardBalance(userId);
    }
}
