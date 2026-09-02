package com.example.cashback.referral.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "referrals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID referrerId;

    @Column(nullable = false)
    private UUID referredUserId;

    private BigDecimal bonusAmount;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
