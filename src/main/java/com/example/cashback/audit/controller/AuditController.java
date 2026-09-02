package com.example.cashback.audit.controller;

import com.example.cashback.audit.model.AuditEvent;
import com.example.cashback.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventRepository repository;

    @PreAuthorize("hasRole('COMPLIANCE') or hasRole('ADMIN')")
    @GetMapping("/events")
    public ResponseEntity<Page<AuditEvent>> getAllEvents(Pageable pageable) {
        if (pageable.getPageSize() > 200) {
            throw new IllegalArgumentException("Page size must not exceed 200");
        }
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @PreAuthorize("hasRole('COMPLIANCE') or hasRole('ADMIN')")
    @GetMapping("/events/{eventType}")
    public ResponseEntity<Page<AuditEvent>> getEventsByType(@PathVariable String eventType, Pageable pageable) {
        if (pageable.getPageSize() > 200) {
            throw new IllegalArgumentException("Page size must not exceed 200");
        }
        return ResponseEntity.ok(repository.findByEventType(eventType, pageable));
    }

    @PreAuthorize("hasRole('COMPLIANCE') or hasRole('ADMIN')")
    @GetMapping("/events/id/{id}")
    public ResponseEntity<AuditEvent> getEventById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
