package com.example.cashback.user.event;

import java.util.UUID;
import java.math.BigDecimal;

public record CashbackBalanceChangedEvent(UUID userId, BigDecimal balance) {
}