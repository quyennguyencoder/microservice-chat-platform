CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(500),
    reference_id UUID,
    reference_type VARCHAR(20),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_notification_user_id ON notifications(user_id);
CREATE INDEX idx_notification_user_id_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notification_created_at ON notifications(created_at);

CREATE TABLE chat_participant_cache (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT uk_chat_participant UNIQUE (chat_id, user_id)
);

CREATE TABLE group_chat_mapping (
    group_id UUID PRIMARY KEY,
    chat_id UUID NOT NULL UNIQUE
);
