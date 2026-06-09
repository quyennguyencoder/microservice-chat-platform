-- ============================================================
-- V1.0__Initial_schema.sql
-- conversation-service — conversation_db
--
-- Tables:
--   chats              — private & group chat containers
--   chat_participants  — users in each chat
--   groups             — group metadata
--   group_members      — group membership & roles
-- ============================================================

-- ─── chats ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS chats (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    type       VARCHAR(10) NOT NULL,         -- PRIVATE | GROUP
    group_id   UUID        UNIQUE,           -- null for PRIVATE chats
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT pk_chats PRIMARY KEY (id),
    CONSTRAINT chk_chats_type CHECK (type IN ('PRIVATE', 'GROUP'))
);

-- ─── chat_participants ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS chat_participants (
    id        UUID      NOT NULL DEFAULT gen_random_uuid(),
    chat_id   UUID      NOT NULL,
    user_id   UUID      NOT NULL,
    joined_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_chat_participants     PRIMARY KEY (id),
    CONSTRAINT fk_cp_chat              FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE,
    CONSTRAINT uq_chat_participant      UNIQUE (chat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_participants_chat_id ON chat_participants (chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_user_id ON chat_participants (user_id);

-- ─── groups ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS groups (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(500),
    avatar_url  VARCHAR(1000),
    created_by  UUID          NOT NULL,
    chat_id     UUID,                        -- filled async after chat-created event
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP,

    CONSTRAINT pk_groups PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_groups_created_by ON groups (created_by);

-- ─── group_members ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS group_members (
    id        UUID        NOT NULL DEFAULT gen_random_uuid(),
    group_id  UUID        NOT NULL,
    user_id   UUID        NOT NULL,
    role      VARCHAR(10) NOT NULL,          -- OWNER | ADMIN | MEMBER
    joined_at TIMESTAMP   NOT NULL,

    CONSTRAINT pk_group_members  PRIMARY KEY (id),
    CONSTRAINT fk_gm_group       FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE,
    CONSTRAINT uq_group_member   UNIQUE (group_id, user_id),
    CONSTRAINT chk_gm_role       CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

CREATE INDEX IF NOT EXISTS idx_group_members_group_id ON group_members (group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_user_id  ON group_members (user_id);
