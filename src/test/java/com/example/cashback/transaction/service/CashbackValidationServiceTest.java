package com.example.cashback.transaction.service;

import com.example.cashback.audit.service.AuditEventService;
import com.example.cashback.transaction.model.Transaction;
import com.example.cashback.transaction.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CashbackValidationServiceTest {

    private final AuditEventService auditEventService = mock(AuditEventService.class);
    private final CashbackValidationService validationService = new CashbackValidationService(auditEventService);

    @Test
    void acceptsCashbackThatMatchesSpend() {
        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.AIRTIME)
                .cashbackAmount(new BigDecimal("2.50"))
                .build();

        assertTrue(validationService.validate(transaction));
    }

    @Test
    void flagsCashbackThatDoesNotMatchSpend() {
        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.AIRTIME)
                .cashbackAmount(new BigDecimal("25.00"))
                .build();

        assertFalse(validationService.validate(transaction));
        verify(auditEventService).recordEventIfAbsent(
                "CASHBACK_ANOMALY",
                "CashbackValidationService",
                "MANUAL_REVIEW_REQUIRED transactionId=null; violations=cashback credit does not match the transaction spend");
    }
}
