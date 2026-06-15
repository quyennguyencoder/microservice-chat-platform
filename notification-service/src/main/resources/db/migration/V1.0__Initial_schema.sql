CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_id VARCHAR(255) NOT NULL,
    actor_id VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    aggregated_count INTEGER NOT NULL DEFAULT 1,
    unique_actor_count INTEGER NOT NULL DEFAULT 1,
    reference_id UUID,
    secondary_ref_id UUID,
    preview_text VARCHAR(200),
    preview_image_id VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notification_recipient ON notifications(recipient_id);
CREATE INDEX idx_notification_recipient_status ON notifications(recipient_id, status);
CREATE INDEX idx_notification_recipient_updated ON notifications(recipient_id, updated_at DESC);
CREATE INDEX idx_notification_aggregation ON notifications(recipient_id, type, reference_id, status);
CREATE INDEX idx_notification_follow_aggregation ON notifications(recipient_id, type, status);

CREATE TABLE notification_actors (
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    actor_id VARCHAR(255),
    position INTEGER
);

CREATE INDEX idx_notification_actors_actor ON notification_actors(actor_id);

CREATE TABLE notification_all_actors (
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    actor_id VARCHAR(255)
);

CREATE INDEX idx_notification_all_actors ON notification_all_actors(notification_id, actor_id);
