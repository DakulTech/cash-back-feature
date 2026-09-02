package com.example.cashback.rewards.repository;

import com.example.cashback.rewards.model.Reward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardRepository extends JpaRepository<Reward, UUID> {

    // Find rewards by user
    List<Reward> findByUserId(UUID userId);

}
