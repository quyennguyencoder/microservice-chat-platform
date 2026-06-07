
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(60),                          -- BCrypt always 60 chars, nullable for OAuth
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    google_id VARCHAR(255) UNIQUE,                      -- Nullable, only for Google OAuth users
    role VARCHAR(20) NOT NULL DEFAULT 'USER',           -- USER or ADMIN
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for frequently queried fields
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);

-- ════════════════════════════════════════════════════════════════════════
-- REFRESH_TOKENS TABLE
-- ════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY NOT NULL,
    token VARCHAR(512) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for frequently queried fields
CREATE INDEX IF NOT EXISTS idx_rt_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_rt_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_rt_expiry_date ON refresh_tokens(expiry_date);

-- ════════════════════════════════════════════════════════════════════════
-- COMMENTS
-- ════════════════════════════════════════════════════════════════════════
COMMENT ON TABLE users IS 'Auth Service User table — stores authentication credentials';
COMMENT ON TABLE refresh_tokens IS 'Refresh token table — stores session tokens with revocation support';
COMMENT ON COLUMN users.password_hash IS 'Nullable for Google OAuth users';
COMMENT ON COLUMN users.google_id IS 'Unique identifier from Google Auth provider';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Soft delete — token is logically revoked';

