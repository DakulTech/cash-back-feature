CREATE TABLE merchant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    country VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_merchant_name ON merchant(name);
CREATE INDEX idx_merchant_category ON merchant(category);
CREATE INDEX idx_merchant_country ON merchant(country);
CREATE INDEX idx_merchant_created_at ON merchant(created_at);
