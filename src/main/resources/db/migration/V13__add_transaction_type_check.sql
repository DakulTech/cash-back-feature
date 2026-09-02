ALTER TABLE transactions
ADD CONSTRAINT chk_transaction_type
CHECK (type IN ('TRANSFER', 'AIRTIME', 'DATA', 'RESTAURANT'));
