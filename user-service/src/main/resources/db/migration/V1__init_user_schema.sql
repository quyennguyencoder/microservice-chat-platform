-- ZChat User Service - Initial Schema
-- Aligned with zchat-system-design.md

-- ==================== Users Table ====================
CREATE TABLE IF NOT EXISTS users (
    id              VARCHAR(255)    PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    display_name    VARCHAR(100),
    image_id        VARCHAR(100),
    banner_image_id VARCHAR(100),
    description     VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ==================== Friendships Table ====================
CREATE TABLE IF NOT EXISTS friendships (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id    VARCHAR(255)    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id    VARCHAR(255)    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status          VARCHAR(20)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,

    CONSTRAINT uk_friendship_requester_addressee UNIQUE (requester_id, addressee_id),
    CONSTRAINT chk_friendship_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'BLOCKED')),
    CONSTRAINT chk_friendship_not_self CHECK (requester_id <> addressee_id)
);

CREATE INDEX IF NOT EXISTS idx_friendship_addressee ON friendships(addressee_id);
CREATE INDEX IF NOT EXISTS idx_friendship_status ON friendships(status);
CREATE INDEX IF NOT EXISTS idx_friendship_requester_status ON friendships(requester_id, status);
CREATE INDEX IF NOT EXISTS idx_friendship_addressee_status ON friendships(addressee_id, status);
