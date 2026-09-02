package com.example.cashback.audit.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String eventType; // e.g. NEGATIVE_BALANCE, FRAUD_ALERT, JOB_FAILURE

    @Column(nullable = false)
    private String source; // e.g. AuditJob, TransactionService

    @Column(length = 2000)
    private String details; // free-form JSON or text

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
