ALTER TABLE user_roles
ADD CONSTRAINT chk_user_roles_role
CHECK (role IN ('USER', 'MERCHANT', 'ADMIN', 'COMPLIANCE'));
