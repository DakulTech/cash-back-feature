CREATE TABLE referral_archive (
    id UUID PRIMARY KEY,
    referrer_id UUID NOT NULL,
    referee_id UUID NOT NULL,
    reward_points INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Indexes
CREATE INDEX idx_referral_archive_referrer ON referral_archive(referrer_id);
CREATE INDEX idx_referral_archive_referee ON referral_archive(referee_id);
CREATE INDEX idx_referral_archive_created_at ON referral_archive(created_at);
