-- ==============================================================================
-- USER SERVICE SCHEMA
-- ==============================================================================

CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY NOT NULL,                       -- Matches users.id from auth-service (1:1 relation)
    email VARCHAR(255) UNIQUE NOT NULL,                 -- Cached from auth-service for quick search/lookup
    display_name VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    bio TEXT,
    gender VARCHAR(20),
    date_of_birth DATE,
    status_message VARCHAR(255),                        -- E.g. "Busy", "At work", etc.
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    last_active_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for frequent queries (like searching for users by email or display name)
CREATE INDEX IF NOT EXISTS idx_user_profiles_email ON user_profiles(email);
CREATE INDEX IF NOT EXISTS idx_user_profiles_display_name ON user_profiles(display_name);
CREATE INDEX IF NOT EXISTS idx_user_profiles_is_online ON user_profiles(is_online);

-- ==============================================================================
-- USER SETTINGS TABLE (Optional but recommended for a chat app)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS user_settings (
    user_id UUID PRIMARY KEY NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL DEFAULT 'vi',         -- e.g. 'vi', 'en'
    theme VARCHAR(20) NOT NULL DEFAULT 'light',         -- e.g. 'light', 'dark', 'system'
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    read_receipts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- COMMENTS
-- ==============================================================================
COMMENT ON TABLE user_profiles IS 'User Service Profile table — stores detailed user information';
COMMENT ON COLUMN user_profiles.id IS 'Primary Key, perfectly matches users.id from auth-service';
COMMENT ON TABLE user_settings IS 'User preferences and settings';
