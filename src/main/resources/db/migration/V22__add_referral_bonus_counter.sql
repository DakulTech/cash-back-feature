CREATE TABLE referral_bonus_totals (
    referrer_id UUID PRIMARY KEY REFERENCES users(id),
    total_bonus NUMERIC(18,2) NOT NULL DEFAULT 0,
    CONSTRAINT chk_referral_bonus_total_non_negative CHECK (total_bonus >= 0)
);

INSERT INTO referral_bonus_totals(referrer_id, total_bonus)
SELECT referrer_id, COALESCE(SUM(reward_points), 0)
FROM referral
GROUP BY referrer_id
ON CONFLICT (referrer_id) DO NOTHING;

CREATE UNIQUE INDEX IF NOT EXISTS uq_referral_referred_user
ON referral(referee_id);