CREATE TABLE financial_book (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    week_start DATE NOT NULL,
    week_end DATE NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    total_settlement NUMERIC(18,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for compliance and reporting
CREATE INDEX idx_financial_book_week ON financial_book(week_start, week_end);
CREATE INDEX idx_financial_book_merchant ON financial_book(merchant_id);
