ALTER TABLE transactions
    ADD COLUMN idempotency_key VARCHAR(128);

UPDATE transactions
SET idempotency_key = 'legacy-' || id::text
WHERE idempotency_key IS NULL;

ALTER TABLE transactions
    ALTER COLUMN idempotency_key SET NOT NULL;

ALTER TABLE transactions
    ADD CONSTRAINT uq_transactions_user_idempotency_key
    UNIQUE (user_id, idempotency_key);