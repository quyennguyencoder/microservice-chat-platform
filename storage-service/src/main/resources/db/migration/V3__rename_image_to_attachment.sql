-- Rename the table
ALTER TABLE image RENAME TO attachment;

-- Make width and height nullable
ALTER TABLE attachment ALTER COLUMN width DROP NOT NULL;
ALTER TABLE attachment ALTER COLUMN height DROP NOT NULL;

-- Rename indexes
ALTER INDEX idx_image_status RENAME TO idx_attachment_status;
ALTER INDEX idx_image_category_status RENAME TO idx_attachment_category_status;
ALTER INDEX idx_image_created_at RENAME TO idx_attachment_created_at;
