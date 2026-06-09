CREATE TABLE user_status (
    user_id UUID PRIMARY KEY,
    status VARCHAR(10) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
