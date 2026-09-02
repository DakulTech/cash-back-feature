package com.example.cashback.transaction.service;

import com.example.cashback.audit.service.AuditEventService;
import com.example.cashback.transaction.model.Transaction;
import com.example.cashback.transaction.model.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashbackValidationService {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private final AuditEventService auditEventService;

    public boolean validate(Transaction transaction) {
        List<String> violations = new ArrayList<>();

        if (transaction.getAmount() == null || transaction.getAmount().signum() <= 0) {
            violations.add("user debit must be greater than zero");
        }

        if (transaction.getType() == null) {
            violations.add("transaction type is missing");
        }

        if (transaction.getCashbackAmount() == null) {
            violations.add("cashback credit is missing");
        }

        if (violations.isEmpty()) {
            BigDecimal spend = money(transaction.getAmount());
            BigDecimal cashback = money(transaction.getCashbackAmount());
            BigDecimal expectedCashback = money(calculateCashback(transaction.getType(), transaction.getAmount()));
            BigDecimal merchantPayout = spend.subtract(cashback);

            if (cashback.compareTo(BigDecimal.ZERO) < 0) {
                violations.add("cashback credit is negative");
            }
            if (cashback.subtract(expectedCashback).abs().compareTo(TOLERANCE) > 0) {
                violations.add("cashback credit does not match the transaction spend");
            }
            if (merchantPayout.compareTo(BigDecimal.ZERO) < 0) {
                violations.add("merchant payout exceeds the user debit");
            }
            if (merchantPayout.compareTo(spend) > 0) {
                violations.add("merchant payout exceeds the user debit");
            }
        }

        if (!violations.isEmpty()) {
            auditEventService.recordEventIfAbsent(
                    "CASHBACK_ANOMALY",
                    "CashbackValidationService",
                    "MANUAL_REVIEW_REQUIRED transactionId=" + transaction.getId()
                            + "; violations=" + String.join(", ", violations));
            return false;
        }

        return true;
    }

    public BigDecimal calculateCashback(TransactionType type, BigDecimal amount) {
        return switch (type) {
            case TRANSFER -> new BigDecimal("10.00");
            case AIRTIME, DATA -> amount.multiply(new BigDecimal("0.025"));
            case RESTAURANT -> amount.multiply(new BigDecimal("0.10"));
        };
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}