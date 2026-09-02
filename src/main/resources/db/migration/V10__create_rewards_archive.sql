CREATE TABLE rewards_archive (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    reward_points INT NOT NULL,
    reward_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Indexes
CREATE INDEX idx_rewards_archive_user ON rewards_archive(user_id);
CREATE INDEX idx_rewards_archive_transaction ON rewards_archive(transaction_id);
CREATE INDEX idx_rewards_archive_created_at ON rewards_archive(created_at);
