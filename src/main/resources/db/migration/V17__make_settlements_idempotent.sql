CREATE UNIQUE INDEX IF NOT EXISTS uq_financial_book_week_merchant
ON financial_book(week_start, week_end, merchant_id);