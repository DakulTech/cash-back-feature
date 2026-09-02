package com.example.cashback.merchant.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;

@Entity
@Table(name = "merchant_offers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 1, fraction = 4)
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal cashbackRate; // e.g. 0.10 for 10%

    private LocalDate expiryDate;

    @Builder.Default
    private Boolean active = true;
}
