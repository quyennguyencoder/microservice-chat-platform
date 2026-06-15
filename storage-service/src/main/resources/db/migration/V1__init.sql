CREATE TABLE image (
    id VARCHAR(36) PRIMARY KEY,
    object_name VARCHAR(255) NOT NULL,
    bucket_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255),
    content_type VARCHAR(255),
    file_size BIGINT,
    category VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    thumbnail_image_id VARCHAR(255),
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    confirmed_at TIMESTAMP
);

CREATE INDEX idx_image_status ON image(status);
CREATE INDEX idx_image_category_status ON image(category, status);
CREATE INDEX idx_image_created_at ON image(created_at);
