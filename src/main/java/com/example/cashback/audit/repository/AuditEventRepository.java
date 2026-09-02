package com.example.cashback.audit.repository;

import com.example.cashback.audit.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    Page<AuditEvent> findByEventType(String eventType, Pageable pageable);

    boolean existsByEventTypeAndSourceAndDetails(String eventType, String source, String details);
}
