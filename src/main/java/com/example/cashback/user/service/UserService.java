package com.example.cashback.user.service;

import com.example.cashback.user.model.User;
import com.example.cashback.user.repository.UserRepository;
import com.example.cashback.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;

    public User register(User user) {
        user.setId(null);
        user.setEmail(user.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        user.setRole(com.example.cashback.user.model.Role.USER);
        user.setReferredBy(null);
        user.setRewardBalance(BigDecimal.ZERO);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findByReferralCode(String referralCode) {
        return userRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new RuntimeException("Referral code not found"));
    }

    public User findByIdForUpdate(UUID id) {
        return userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void creditReward(User user, BigDecimal amount) {
        user.setRewardBalance(user.getRewardBalance().add(amount));
        User savedUser = userRepository.save(user);
        outboxService.enqueue(new com.example.cashback.user.event.CashbackBalanceChangedEvent(
                savedUser.getId(), savedUser.getRewardBalance()));
    }

    @Cacheable(value = "cashbackBalances", key = "#userId", unless = "#result == null")
    public BigDecimal getRewardBalance(UUID userId) {
        return findById(userId).getRewardBalance();
    }
}
