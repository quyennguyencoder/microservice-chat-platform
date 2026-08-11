-- Rename the column in chats table
ALTER TABLE chats RENAME COLUMN image_id TO attachment_id;

-- Rename the column in messages table
ALTER TABLE messages RENAME COLUMN image_id TO attachment_id;
