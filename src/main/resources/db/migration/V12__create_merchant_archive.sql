CREATE TABLE merchant_archive (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    country VARCHAR(100),
    created_at TIMESTAMP NOT NULL
);

-- Indexes
CREATE INDEX idx_merchant_archive_name ON merchant_archive(name);
CREATE INDEX idx_merchant_archive_category ON merchant_archive(category);
CREATE INDEX idx_merchant_archive_country ON merchant_archive(country);
CREATE INDEX idx_merchant_archive_created_at ON merchant_archive(created_at);
