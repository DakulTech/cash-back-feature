package com.example.cashback.transaction.dto;

import com.example.cashback.merchant.model.Merchant;
import com.example.cashback.merchant.model.MerchantOffer;
import com.example.cashback.transaction.model.Transaction;
import com.example.cashback.transaction.model.TransactionType;
import com.example.cashback.user.model.User;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull User user,
        @NotNull Merchant merchant,
        MerchantOffer offer,
        @Positive @Digits(integer = 16, fraction = 2) BigDecimal amount,
        TransactionType type,
        @NotBlank @jakarta.validation.constraints.Size(max = 128) String idempotencyKey) {

    public Transaction toTransaction() {
        return Transaction.builder()
                .user(user)
                .merchant(merchant)
                .offer(offer)
                .amount(amount)
                .type(type)
                .idempotencyKey(idempotencyKey)
                .build();
    }
}