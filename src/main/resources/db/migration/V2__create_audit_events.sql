-- Create partitioned table
CREATE TABLE audit_events (
    id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    source VARCHAR(255) NOT NULL,
    details TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);

-- Create initial monthly partitions
CREATE TABLE audit_events_2026_08 PARTITION OF audit_events
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

CREATE TABLE audit_events_2026_09 PARTITION OF audit_events
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');

-- Indexes on each partition
CREATE INDEX idx_audit_events_event_type_2026_08 ON audit_events_2026_08(event_type);
CREATE INDEX idx_audit_events_event_type_2026_09 ON audit_events_2026_09(event_type);

CREATE INDEX idx_audit_events_source_2026_08 ON audit_events_2026_08(source);
CREATE INDEX idx_audit_events_source_2026_09 ON audit_events_2026_09(source);

-- Composite index for combined queries
CREATE INDEX idx_audit_events_type_timestamp_2026_08
    ON audit_events_2026_08(event_type, timestamp);

CREATE INDEX idx_audit_events_type_timestamp_2026_09
    ON audit_events_2026_09(event_type, timestamp);
