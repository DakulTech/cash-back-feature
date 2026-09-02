CREATE TABLE transactions_archive (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Indexes for time-range queries
CREATE INDEX idx_transactions_archive_user ON transactions_archive(user_id);
CREATE INDEX idx_transactions_archive_merchant ON transactions_archive(merchant_id);
CREATE INDEX idx_transactions_archive_created_at ON transactions_archive(created_at);
