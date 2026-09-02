package com.example.cashback.audit.service;

import com.example.cashback.audit.model.AuditEvent;
import com.example.cashback.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final AuditEventRepository repository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public AuditEvent recordEvent(String type, String source, String details) {
        AuditEvent event = AuditEvent.builder()
                .eventType(type)
                .source(source)
                .details(details)
                .build();
        AuditEvent result = repository.save(event);
        return result;
    }

    @Transactional
    public AuditEvent recordEventIfAbsent(String type, String source, String details) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO audit_event_fingerprints (fingerprint, created_at)
                VALUES (:fingerprint, CURRENT_TIMESTAMP)
                ON CONFLICT (fingerprint) DO NOTHING
                """, Map.of("fingerprint", fingerprint(type, source, details)));
        if (inserted == 0) {
            return null;
        }
        return recordEvent(type, source, details);
    }

    private String fingerprint(String type, String source, String details) {
        try {
            byte[] input = (type + "\u0000" + source + "\u0000" + (details == null ? "" : details))
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
