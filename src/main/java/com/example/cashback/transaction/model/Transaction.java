package com.example.cashback.transaction.model;

import jakarta.persistence.*;
import lombok.*;
import com.example.cashback.user.model.User;
import com.example.cashback.merchant.model.Merchant;
import com.example.cashback.merchant.model.MerchantOffer;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user; // replaces raw userId

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @NotNull
    private Merchant merchant; // replaces raw merchantId

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id")
    private MerchantOffer offer;

    @Positive
    @Digits(integer = 16, fraction = 2)
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // TRANSFER, AIRTIME, DATA, RESTAURANT

    @Digits(integer = 16, fraction = 2)
    @Column(precision = 18, scale = 2)
    private BigDecimal cashbackAmount;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
