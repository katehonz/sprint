-- V9: Fix superadmin password hash
-- The original hash in V8 was incorrect. This updates it to the correct hash.
-- IMPORTANT: Change the default password immediately after first login!

UPDATE users
SET password_hash = '$2b$12$CzzH9KBptDSThQtWVzWvWuNXA/Cln8I4UHNn.2Y3kYlsJNqCnCoyK'
WHERE username = 'superadmin';
