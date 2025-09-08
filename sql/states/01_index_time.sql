-- Create time index to improve recent orders queries
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at);
ANALYZE orders;
