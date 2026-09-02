package com.example.cashback.rewards.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "rewards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Positive
    @Digits(integer = 16, fraction = 2)
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    private String description; // e.g. "Cashback on Airtime", "Referral Bonus"

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
