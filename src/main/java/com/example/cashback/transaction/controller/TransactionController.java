package com.example.cashback.transaction.controller;

import com.example.cashback.transaction.dto.TransactionAnalyticsDTO;
import com.example.cashback.transaction.dto.TransactionRequest;
import com.example.cashback.transaction.model.Transaction;
import com.example.cashback.transaction.service.TransactionService;
import com.example.cashback.transaction.service.TransactionAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionAnalyticsService transactionAnalyticsService;

    @PostMapping
    @PreAuthorize("@accessControl.canAccessUser(#request.user.id)")
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.processTransaction(request));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@accessControl.canAccessUser(#userId)")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable UUID userId) {
        return ResponseEntity.ok(transactionService.getUserTransactions(userId));
    }

    // Unified analytics endpoint
    @GetMapping("/analytics")
    public ResponseEntity<List<TransactionAnalyticsDTO>> getTransactionAnalytics() {
        return ResponseEntity.ok(transactionAnalyticsService.getTransactionAnalytics());
    }
}
