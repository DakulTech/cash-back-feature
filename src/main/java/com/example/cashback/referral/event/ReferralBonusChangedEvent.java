package com.example.cashback.referral.event;

import java.math.BigDecimal;

public record ReferralBonusChangedEvent(String referralCode, BigDecimal totalBonus) {
}