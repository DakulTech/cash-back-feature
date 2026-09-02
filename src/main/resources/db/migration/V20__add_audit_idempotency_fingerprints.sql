CREATE TABLE audit_event_fingerprints (
    fingerprint CHAR(64) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_event_fingerprints_created_at
ON audit_event_fingerprints(created_at);