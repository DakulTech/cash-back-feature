-- Transactions → Merchant
ALTER TABLE transactions
ADD CONSTRAINT fk_transactions_merchant
FOREIGN KEY (merchant_id) REFERENCES merchant(id);

-- Rewards → Transactions
ALTER TABLE rewards
ADD CONSTRAINT fk_rewards_transaction
FOREIGN KEY (transaction_id) REFERENCES transactions(id);

-- Rewards → User (assuming you have a users table)
ALTER TABLE rewards
ADD CONSTRAINT fk_rewards_user
FOREIGN KEY (user_id) REFERENCES users(id);

-- Referral → User (referrer and referee both point to users table)
ALTER TABLE referral
ADD CONSTRAINT fk_referral_referrer
FOREIGN KEY (referrer_id) REFERENCES users(id);

ALTER TABLE referral
ADD CONSTRAINT fk_referral_referee
FOREIGN KEY (referee_id) REFERENCES users(id);

-- Transactions → User
ALTER TABLE transactions
ADD CONSTRAINT fk_transactions_user
FOREIGN KEY (user_id) REFERENCES users(id);
