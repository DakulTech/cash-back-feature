CREATE TABLE referral (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_id UUID NOT NULL,
    referee_id UUID NOT NULL,
    reward_points INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_referral_referrer ON referral(referrer_id);
CREATE INDEX idx_referral_referee ON referral(referee_id);
CREATE INDEX idx_referral_status ON referral(status);
CREATE INDEX idx_referral_created_at ON referral(created_at);
