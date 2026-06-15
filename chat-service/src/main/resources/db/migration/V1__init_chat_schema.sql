CREATE TABLE chats (
    id UUID PRIMARY KEY,
    type VARCHAR(10) NOT NULL,
    name VARCHAR(100),
    image_id VARCHAR(100),
    owner_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE chat_members (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_chat_members_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE,
    CONSTRAINT uk_chat_member UNIQUE (chat_id, user_id)
);

CREATE INDEX idx_chat_member_user ON chat_members (user_id);
CREATE INDEX idx_chat_member_chat ON chat_members (chat_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL,
    sender_id VARCHAR(255) NOT NULL,
    content TEXT,
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    state VARCHAR(20) NOT NULL DEFAULT 'SENT',
    image_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE
);

CREATE INDEX idx_message_chat ON messages (chat_id);
CREATE INDEX idx_message_chat_created ON messages (chat_id, created_at);
