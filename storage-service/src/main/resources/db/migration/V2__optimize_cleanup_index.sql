-- Tạo Composite Index để tối ưu truy vấn dọn rác (lọc theo status và created_at)
CREATE INDEX IF NOT EXISTS idx_image_status_created_at ON image(status, created_at);
