ALTER TABLE users
    ALTER COLUMN reward_balance TYPE NUMERIC(18,2)
    USING ROUND(reward_balance::numeric, 2);

ALTER TABLE users
    ADD CONSTRAINT chk_users_reward_balance_non_negative CHECK (reward_balance >= 0);

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0);

CREATE INDEX IF NOT EXISTS idx_audit_events_timestamp ON audit_events(timestamp);