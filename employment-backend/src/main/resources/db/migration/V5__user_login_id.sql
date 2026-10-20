ALTER TABLE users ADD COLUMN login_id VARCHAR(30);
CREATE UNIQUE INDEX uq_users_login_id ON users(login_id);
