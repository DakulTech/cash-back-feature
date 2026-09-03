package com.example.cashback.outbox.service;

public record ReconciliationSummary(long transactionCount, long anomaliesDetected, long deadLetterCount) {
}