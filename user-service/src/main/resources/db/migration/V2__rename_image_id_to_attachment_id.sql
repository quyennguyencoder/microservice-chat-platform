-- Rename image_id to attachment_id
ALTER TABLE users RENAME COLUMN image_id TO attachment_id;

-- Rename banner_image_id to banner_attachment_id
ALTER TABLE users RENAME COLUMN banner_image_id TO banner_attachment_id;
