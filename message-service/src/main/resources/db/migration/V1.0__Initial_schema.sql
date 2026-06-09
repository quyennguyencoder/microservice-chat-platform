CREATE TABLE messages (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_message_chat_id ON messages(chat_id);
CREATE INDEX idx_message_sender_id ON messages(sender_id);
CREATE INDEX idx_message_created_at ON messages(created_at);
