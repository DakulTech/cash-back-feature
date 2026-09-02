ALTER TABLE transactions
ADD COLUMN offer_id UUID;

ALTER TABLE transactions
ADD CONSTRAINT fk_transactions_offer
FOREIGN KEY (offer_id) REFERENCES merchant_offers(id);

CREATE INDEX idx_transactions_offer ON transactions(offer_id);